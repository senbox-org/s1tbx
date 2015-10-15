/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.dataio.envisat;


import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>HeaderParser</code> class is used to parse the ASCII contents of ENVISAT main and specific product headers
 * (MPH and SPH) in order to create instances of the <code>Header</code> class.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.Header
 */
public class HeaderParser {

    /**
     * Gets the singleton instance of a ENVISAT header parser.
     */
    public static HeaderParser getInstance() {
        return Holder.instance;
    }

    /**
     * Constructs a new record containing the parameters in the ASCII header source. Each record field stands for a
     * single header parameter.
     * <p>
     * The source is expected to be a sequence of new-line separated records of key-value pairs each having the form
     * <code><i>key</i>=<i>value</i> <i>new-line</i></code>. The exact ENVISAT header format is given in the document
     * <i>ENVISAT-1 PRODUCTS SPECIFICATION, Volume 5: Product Structures.</i>
     *
     * @param headerName  the name of the header, e.g. <code>"MPH"</code> or <code>"SPH"</code>
     * @param sourceBytes the source
     *
     * @return an object representing an ENVISAT header
     *
     * @throws org.esa.snap.dataio.envisat.HeaderParseException
     *          if a parsing exception occurs
     */
    public Header parseHeader(String headerName, byte[] sourceBytes) throws HeaderParseException {
        Guardian.assertNotNullOrEmpty("headerName", headerName);
        Guardian.assertNotNullOrEmpty("sourceBytes", sourceBytes);
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw new IllegalArgumentException("sourceBytes array must not be null or empty"); /*I18N*/
        }

        LineNumberReader reader = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(sourceBytes)));
        RecordInfo recordInfo = new RecordInfo(headerName);
        List recordTokens = new ArrayList();
        String line;

        try {
            while ((line = reader.readLine()) != null) {

                line = line.trim();
                if (line.length() == 0) {
                    // Ignore empty lines
                    continue;
                }

                int assignPos = line.indexOf('=');
                if (assignPos <= 0) {
                    error(headerName, reader, "invalid header entry found: " + line);
                }

                String name = line.substring(0, assignPos).trim();
                String value = line.substring(assignPos + 1).trim();
                String unit = null;

                int unitStartPos = value.lastIndexOf('<');
                int unitEndPos = value.lastIndexOf('>');
                if (unitStartPos > 0
                    && unitEndPos == value.length() - 1
                    && unitStartPos < unitEndPos) {
                    unit = value.substring(unitStartPos + 1, unitEndPos);
                    value = value.substring(0, unitStartPos);
                }

                FieldInfo fieldInfo = createFieldInfo(name, value, unit);
                recordInfo.add(fieldInfo);
                recordTokens.add(value);
            }
        } catch (HeaderParseException e) {
            error(headerName, reader, e.getMessage());
        } catch (IOException e) {
            error(headerName, reader, e.getMessage());
        }

        reader = null;

        Record record = recordInfo.createRecord();
        for (int i = 0; i < record.getNumFields(); i++) {
            Field field = record.getFieldAt(i);
            String token = (String) recordTokens.get(i);
            try {
                setFieldData(field, token);
            } catch (NumberFormatException e) {
                error(headerName, null, "invalid header entry found: " + field.getName() + "=" + token);
            }
        }

        return new Header(record);
    }

    private FieldInfo createFieldInfo(String name, String value, String unit) {
        int dataType = ProductData.TYPE_UNDEFINED;
        int numElems = -1;

        if (value.length() == 1
            && isBooleanDigit(value.charAt(0))) {
            dataType = ProductData.TYPE_INT8;
            numElems = 1;
        } else if (value.length() == 1) {
            dataType = ProductData.TYPE_ASCII;
            numElems = value.length();
        } else if (value.length() >= 2
                   && isQuote(value.charAt(0))
                   && isQuote(value.charAt(value.length() - 1))) {
            dataType = ProductData.TYPE_ASCII;
            numElems = value.length() - 2;
        } else if (value.length() >= 2
                   && isSign(value.charAt(0))
                   && (isDigit(value.charAt(1))
                       || isDot(value.charAt(1)))) {

            numElems = 1;

            int pos = 0;
            int numDigitsMax = -1;
            boolean minusSeen = false;
            boolean dotSeen = false;
            boolean expPartSeen = false;
            boolean notANumber = false;
            long intNum = 0;
            long intNumMax = 0;

            do {
                if (value.charAt(pos) == '-') {
                    minusSeen = true;
                }
                pos++;

                int numDigits = 0;
                while (pos < value.length()) {
                    char ch = value.charAt(pos);
                    if (Character.isDigit(ch)) {
                        pos++;
                        numDigits++;
                        numDigitsMax = Math.max(numDigitsMax, numDigits);
                        intNum *= 10L;
                        intNum += (ch - '0');
                        intNumMax = Math.max(intNumMax, intNum);
                    } else if (isDot(ch)) {
                        pos++;
                        dotSeen = true;
                    } else if (isExponentPrefix(ch)) {
                        pos++;
                        if (pos < value.length() - 2
                            && isSign(value.charAt(pos))
                            && isDigit(value.charAt(pos + 1))) {
                            pos += 2;
                            expPartSeen = true;
                        } else {
                            notANumber = true;
                            break;
                        }
                    } else if (isSign(ch)) {
                        // Normal break due to next number element ('+' or '-' are separators)
                        intNum = 0;
                        numElems++;
                        break;
                    } else {
                        notANumber = true;
                        break;
                    }
                }
                if (numDigits == 0) {
                    notANumber = true;
                    break;
                }
            } while (pos < value.length() && isSign(value.charAt(pos)));

            if (!notANumber) {
                if (dotSeen || expPartSeen) {
                    dataType = ProductData.TYPE_FLOAT64;
                } else if (!minusSeen && (intNumMax > Integer.MAX_VALUE || numDigitsMax > 10)) {
                    dataType = ProductData.TYPE_UINT32;
                } else {
                    dataType = ProductData.TYPE_INT32;
                }
            }
        }

        if (dataType == ProductData.TYPE_UNDEFINED) {
            // Actually this is an unknown data type...
            Debug.trace(
                    "WARNING: header parameter with unknown value type seen: " + name + "='" + value + "' (assuming type string)");
            dataType = ProductData.TYPE_ASCII;
            numElems = value.length();
        }

        Debug.assertTrue(numElems >= 0);

        return new FieldInfo(name, dataType, numElems, unit, null);
    }

    private void setFieldData(Field field, String token) throws NumberFormatException {

        Debug.assertNotNull(field);
        Debug.assertNotNull(token);

        switch (field.getInfo().getDataType()) {
        case ProductData.TYPE_INT8:
        case ProductData.TYPE_UINT8: {
            int pos1 = 0;
            byte[] values = (byte[]) field.getElems();
            for (int i = 0; i < values.length; i++) {
                int pos2 = getNextNumberSeparatorPos(token, pos1 + 1);
                values[i] = (byte) parseLong(token.substring(pos1, pos2));
                pos1 = pos2;
            }
        }
        break;
        case ProductData.TYPE_INT16:
        case ProductData.TYPE_UINT16: {
            int pos1 = 0;
            short[] values = (short[]) field.getElems();
            for (int i = 0; i < values.length; i++) {
                int pos2 = getNextNumberSeparatorPos(token, pos1 + 1);
                values[i] = (short) parseLong(token.substring(pos1, pos2));
                pos1 = pos2;
            }
        }
        break;
        case ProductData.TYPE_INT32:
        case ProductData.TYPE_UINT32: {
            int pos1 = 0;
            int[] values = (int[]) field.getElems();
            for (int i = 0; i < values.length; i++) {
                int pos2 = getNextNumberSeparatorPos(token, pos1 + 1);
                values[i] = (int) parseLong(token.substring(pos1, pos2));
                pos1 = pos2;
            }
        }
        break;
        case ProductData.TYPE_FLOAT32: {
            int pos1 = 0;
            float[] values = (float[]) field.getElems();
            for (int i = 0; i < values.length; i++) {
                int pos2 = getNextNumberSeparatorPos(token, pos1 + 1);
                values[i] = (float) parseDouble(token.substring(pos1, pos2));
                pos1 = pos2;
            }
        }
        break;
        case ProductData.TYPE_FLOAT64: {
            int pos1 = 0;
            double[] values = (double[]) field.getElems();
            for (int i = 0; i < values.length; i++) {
                int pos2 = getNextNumberSeparatorPos(token, pos1 + 1);
                values[i] = parseDouble(token.substring(pos1, pos2));
                pos1 = pos2;
            }
        }
        break;
        case ProductData.TYPE_ASCII: {
            byte[] values = (byte[]) field.getElems();
            if (token.length() >= 2
                && isQuote(token.charAt(0))
                && isQuote(token.charAt(token.length() - 1))) {
                System.arraycopy(token.getBytes(), 1, values, 0, values.length);
            } else if (token.length() >= 1) {
                System.arraycopy(token.getBytes(), 0, values, 0, values.length);
            }
        }
        break;
        default:
            throw new IllegalStateException("invalid field data type");
        }
    }


    private long parseLong(String value) throws NumberFormatException {
        long sign = 1L;
        int pos = 0;
        // Eat trailing '+' or '-'
        if (value.charAt(0) == '+' || value.charAt(0) == '-') {
            sign = value.charAt(0) == '+' ? 1L : -1L;
            pos++;
        }
        // Skip trailing '0' sequence
        pos = getFirstButNotLastNonZeroPos(value, pos);
        if (pos > 0) {
            value = value.substring(pos);
        }
        return sign * Long.parseLong(value);
    }

    private double parseDouble(String value) throws NumberFormatException {
        double sign = 1.0;
        int pos = 0;
        // Eat trailing '+' or '-'
        if (value.charAt(0) == '+' || value.charAt(0) == '-') {
            sign = value.charAt(0) == '+' ? 1.0 : -1.0;
            pos++;
        }
        // Skip trailing '0' sequence
        pos = getFirstButNotLastNonZeroPos(value, pos);
        if (pos > 0) {
            value = value.substring(pos);
        }
        return sign * Double.parseDouble(value);
    }


    private int getFirstButNotLastNonZeroPos(String value, int pos) {
        for (; pos < value.length() - 1; pos++) {
            if (value.charAt(pos) != '0') {
                break;
            }
        }
        return pos;
    }

    private int getNextNumberSeparatorPos(String value, int pos) {
        for (; pos < value.length(); pos++) {
            if (isSign(value.charAt(pos))
                && !(pos > 0 && isExponentPrefix(value.charAt(pos - 1)))) {
                break;
            }
        }
        return pos;
    }

    private static boolean isSign(char ch) {
        return ch == '+' || ch == '-';
    }

    private static boolean isDigit(char ch) {
        return Character.isDigit(ch);
    }

    private static boolean isExponentPrefix(char ch) {
        return ch == 'e' || ch == 'E';
    }

    private static boolean isBooleanDigit(char ch) {
        return ch == '0' || ch == '1';
    }

    private static boolean isDot(char ch) {
        return ch == '.';
    }

    private static boolean isQuote(char ch) {
        return ch == '"';
    }

    private void error(String headerName, LineNumberReader reader, String msg) throws HeaderParseException {
        StringBuffer sb = new StringBuffer();
        sb.append("ENVISAT header '");
        sb.append(headerName);
        sb.append("'");
        if (reader != null) {
            sb.append(", line ");
            sb.append(reader.getLineNumber());
        }
        sb.append(": ");
        sb.append(msg);
        throw new HeaderParseException(sb.toString());
    }

    private HeaderParser() {
    }

    // Initialization on demand holder idiom
    private static class Holder {
        private static final HeaderParser instance = new HeaderParser();
    }
}


