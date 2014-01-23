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
package org.esa.beam.dataio.modis;

import org.esa.beam.dataio.modis.netcdf.NetCDFVariables;
import org.esa.beam.dataio.modis.productdb.ModisProductDb;
import org.esa.beam.framework.dataio.ProductIOException;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class ModisDaacUtils {

    public static String extractCoreString(NetCDFVariables netCDFVariables) throws IOException {
        final String coreKey = ModisConstants.CORE_META_KEY;
        final String coreString = coreKey.substring(0, coreKey.length() - 2);

        final StringBuilder buffer = new StringBuilder();
        final Variable[] variables = netCDFVariables.getAll();
        for (final Variable variable : variables) {
            final String name = variable.getFullName();
            if (name.startsWith(coreString) && name.length() == coreKey.length()) {
                buffer.append(variable.readScalarString());
            }
        }

        return correctAmpersandWrap(buffer.toString());
    }

    public static String correctAmpersandWrap(final String input) {
        final StringReader reader = new StringReader(input);
        final StringWriter result = new StringWriter();

        boolean ampersandMode = false;
        boolean maybeAmpersandMode = false;
        try {
            for (int current = reader.read(); current > -1; current = reader.read()) {
                if (maybeAmpersandMode) {
                    if (current == '\n' || current == '\r') {
                        ampersandMode = true;
                        maybeAmpersandMode = false;
                    } else {
                        result.write('&');
                        maybeAmpersandMode = false;
                    }
                }
                if (ampersandMode) {
                    if (current == '\n' || current == '\r' || current == ' ') {
                        //skip
                    } else {
                        result.write(current);
                        ampersandMode = false;
                    }
                } else {
                    if (current == '&') {
                        maybeAmpersandMode = true;
                    } else {
                        result.write(current);
                    }
                }
            }
        } catch (IOException e) {
            // ignore because the readers input is a StringReader
        }
        return result.toString();
    }

    public static String extractProductType(String s) throws ProductIOException {
        final ModisProductDb db = ModisProductDb.getInstance();

        ArrayList<String> prodType = extractProductTypeBySeparatorChar(s, db, ".");
        if (prodType.size() == 1) {
            return prodType.get(0);
        }
        prodType = extractProductTypeBySeparatorChar(s, db, "_");
        if (prodType.size() == 1) {
            return prodType.get(0);
        }
        return "";
    }

    private static ArrayList<String> extractProductTypeBySeparatorChar(String s, ModisProductDb db, String delim) throws ProductIOException {
        final ArrayList<String> prodType = new ArrayList<String>();
        final StringTokenizer tokenizer = new StringTokenizer(s, delim);
        while (tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken();
            if (db.isSupportedProduct(token)) {
                prodType.add(token);
            }
        }
        return prodType;
    }
}