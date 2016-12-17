/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.gamma.header;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Parses a Gamma par file
 */
class HeaderParser {

    private final Map<String, String> header;

    private HeaderParser(Map<String, String> header) {
        this.header = header;
    }

    public static HeaderParser parse(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        Map<String, String> header = new LinkedHashMap<>();
        Map<String, String> currentMap = header;
        for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
            if (line.contains(":")) {
                String[] keyValue = line.split(":", 2);
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                if (value.startsWith("{")) {
                    value = value.substring(1).trim();
                    while (!value.endsWith("}")) {
                        String continuedLine = bufferedReader.readLine();
                        value = value + " " + continuedLine.trim();
                        value = value.trim();
                    }
                    value = value.substring(0, value.length() - 1);
                }
                currentMap.put(key, value);
            }
        }
        return new HeaderParser(header);
    }

    private boolean contains(String key) {
        return header.containsKey(key);
    }

    private String getString(String key) {
        if (!contains(key)) {
            throw new IllegalArgumentException("Missing mandatory header key: " + key);
        }
        return header.get(key);
    }

    private String getNumericString(String key) {
        if (!contains(key)) {
            throw new IllegalArgumentException("Missing mandatory header key: " + key);
        }
        String val = header.get(key).trim();
        int lastDigit = val.length();
        if (val.indexOf(' ') > 0) {
            lastDigit = val.indexOf(' ');
        }
        if (val.indexOf('\t') > 0) {
            lastDigit = val.indexOf('\t');
        }
        return val.substring(0, lastDigit);
    }

    String getString(String key, String defaultValue) {
        return contains(key) ? getString(key) : defaultValue;
    }

    String[] getStrings(String key) {
        String v = header.get(key);
        if (v == null) {
            return new String[0];
        } else {
            String[] splits = v.split(",");
            List<String> splitsTrimmed = new ArrayList<>(splits.length);
            for (String split : splits) {
                String trimmed = split.trim();
                if (!trimmed.isEmpty()) {
                    splitsTrimmed.add(trimmed);
                }
            }
            return splitsTrimmed.toArray(new String[splitsTrimmed.size()]);
        }
    }

    int getInt(String key) {
        if (!contains(key)) {
            throw new IllegalArgumentException("Missing mandatory header key: " + key);
        }
        return Integer.parseInt(getNumericString(key));
    }

    int getInt(String key, int defaultValue) {
        return contains(key) ? getInt(key) : defaultValue;
    }

    int[] getInts(String key) {
        String[] elems = getStrings(key);
        int[] ints = new int[elems.length];
        for (int i = 0; i < elems.length; i++) {
            ints[i] = Integer.parseInt(elems[i]);
        }
        return ints;
    }

    double getDouble(String key) {
        if (!contains(key)) {
            throw new IllegalArgumentException("Missing mandatory header key: " + key);
        }
        return Double.parseDouble(getNumericString(key));
    }

    double getDouble(String key, int defaultValue) {
        return contains(key) ? getDouble(key) : defaultValue;
    }

    double[] getDoubles(String key) {
        String[] elems = getStrings(key);
        double[] doubles = new double[elems.length];
        for (int i = 0; i < elems.length; i++) {
            doubles[i] = Double.parseDouble(elems[i]);
        }
        return doubles;
    }

    Set<Map.Entry<String, String>> getHeaderEntries() {
        return header.entrySet();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Header\n");
        mapToString(sb, header);
        return sb.toString();
    }

    private static void mapToString(StringBuilder sb, Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
    }
}
