/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.csv.productio;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
public class CsvProductFile implements CsvProductSourceParser, CsvProductSource {

    private final Map<String, String> properties = new HashMap<String, String>();
    private final List<Record> records = new ArrayList<Record>();
    private final File csv;

    private Header header;
    
    public CsvProductFile(String csv) {
        this(new File(csv));
    }

    public CsvProductFile(File csv) {
        this.csv = csv;
    }

    @Override
    public void parseProperties() throws ParseException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(csv));
            String line;
            while((line = reader.readLine()) != null) {
                if(!line.startsWith("#")) {
                    break;
                }
                line = line.substring(1);
                int pos = line.indexOf('=');
                if (pos == -1) {
                    throw new ParseException("Missing '=' in '" + line + "'");
                }
                String name = line.substring(0, pos).trim();
                if (name.isEmpty()) {
                    throw new ParseException("Empty property name in '" + line + "'");
                }
                String value = line.substring(pos + 1).trim();
                properties.put(name, value);
            }
        } catch (IOException e) {
            throw new ParseException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void parseRecords() throws ParseException {
        if(header == null) {
            throw new IllegalStateException("header needs to be parsed first");
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(csv));
            String line;
            while((line = reader.readLine()) != null) {
                if(!line.startsWith("#")) {
                    break;
                }
            }

            int columnIndex = 0;
            while ((line = reader.readLine()) != null) {
                String locationName = null;
                ProductData.UTC time = null;
                float lat = Float.NaN;
                float lon = Float.NaN;
                List<Object> values = new ArrayList<Object>();
                for (String token : getTokens(line)) {
                    final HeaderImpl.AttributeHeader attributeHeader = header.getAttributeHeader(columnIndex);
                    if(contains(Constants.LOCATION_NAMES, attributeHeader.name)) {
                        locationName = token;
                    } else if(contains(Constants.LAT_NAMES, attributeHeader.name)) {
                        lat = Float.parseFloat(token);
                    } else if(contains(Constants.LON_NAMES, attributeHeader.name)) {
                        lon = Float.parseFloat(token);
                    } else if(contains(Constants.TIME_NAMES, attributeHeader.name)) {
                        if(token != null && !token.isEmpty()) {
                            time = ProductData.UTC.parse(token, Constants.TIME_PATTERN);
                        } else {
                            time = null;
                        }
                    } else {
                        values.add(toObject(token, new Class<?>[]{Double.class, Long.class, ProductData.UTC.class, String.class}));
                    }
                    
                    columnIndex++;
                }
                final Measurement measurement = new Measurement(locationName, time, new GeoPos(lat, lon), values.toArray(new Object[values.size()]));
                records.add(measurement);
                columnIndex = 0;
            }
        } catch (Exception e) {
            throw new ParseException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void parseHeader() throws IOException {
        header = new HeaderImpl(csv);
    }

    @Override
    public List<Record> getRecords() {
        return Collections.unmodifiableList(records);
    }


    @Override
    public Header getHeader() {
        return header;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    private boolean contains(String[] possibleStrings, String s) {
        for (String possibleString : possibleStrings) {
            if(possibleString.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<String> getTokens(String line) {
        int pos2;
        int pos1 = 0;
        final ArrayList<String> strings = new ArrayList<String>();
        while ((pos2 = line.indexOf('\t', pos1)) >= 0) {
            strings.add(line.substring(pos1, pos2).trim());
            pos1 = pos2 + 1;
        }
        strings.add(line.substring(pos1).trim());
        return strings;
    }

    private Object toObject(String textValue, Class<?>[] types) {
        if (textValue != null && !textValue.isEmpty()) {
            for (final Class<?> type : types) {
                Object value = parse(textValue, type);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private static Object parse(String text, Class<?> type) {
        if (type.equals(Double.class)) {
            try {
                return parseDouble(text);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (type.equals(String.class)) {
            return text;
        } else if (type.equals(ProductData.UTC.class)) {
            try {
                return ProductData.UTC.parse(text, Constants.TIME_PATTERN);
            } catch (java.text.ParseException e) {
                return null;
            }
        } else if (type.equals(Long.class)) {
            final long longValue;
            try {
                longValue = Long.parseLong(text);
            } catch (NumberFormatException e) {
                return null;
            }
            return longValue;
        } else {
            throw new IllegalStateException("Unhandled data type: " + type);
        }
    }

    private static Double parseDouble(String text) {
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException e) {
            if (text.equalsIgnoreCase("nan")) {
                return Double.NaN;
            } else if (text.equalsIgnoreCase("inf") || text.equalsIgnoreCase("infinity")) {
                return Double.POSITIVE_INFINITY;
            } else if (text.equalsIgnoreCase("-inf") || text.equalsIgnoreCase("-infinity")) {
                return Double.NEGATIVE_INFINITY;
            } else {
                throw e;
            }
        }
    }

    static class ParseException extends Exception {

        ParseException(String message) {
            super(message);
        }

        private ParseException(Throwable cause) {
            super(cause);
        }

        ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
