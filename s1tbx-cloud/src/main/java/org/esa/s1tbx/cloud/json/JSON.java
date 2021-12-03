/*
 * Copyright (C) 2021 SkyWatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.cloud.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public interface JSON {

    static Object loadJSON(final File file) throws Exception {
        final JSONParser parser = new JSONParser();
        try (FileReader fileReader = new FileReader(file)) {
            return parser.parse(fileReader);
        }
    }

    static Object parse(final String str) throws Exception {
        final JSONParser parser = new JSONParser();
        return parser.parse(str);
    }

    static String prettyPrint(final Object json) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }

    static void write(final Object json, final File file) throws IOException {
        final File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (final FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(prettyPrint(json));
        }
    }

    static boolean equals(final JSONObject obj1, final String jsonStr) throws Exception {
        final JSONParser parser = new JSONParser();
        final JSONObject obj2 = (JSONObject) parser.parse(jsonStr);
        return equals(obj1, obj2);
    }

    static boolean equals(final JSONObject obj1, final JSONObject obj2) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();

        JsonNode tree1 = mapper.readTree(obj1.toString());
        JsonNode tree2 = mapper.readTree(obj2.toString());

        return tree1.equals(tree2);
    }

    static boolean getBoolean(final Object o) {
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return Boolean.parseBoolean((String) o);
    }

    static int getInt(final Object o) {
        if (o instanceof Long) {
            return (int) (long) o;
        } else if (o instanceof Integer) {
            return (Integer) o;
        }
        return Integer.parseInt((String) o);
    }

    static long getLong(final Object o) {
        if (o instanceof Long) {
            return (Long) o;
        } else if (o instanceof Integer) {
            return (Integer) o;
        }
        return Long.parseLong((String) o);
    }

    static double getDouble(final Object o) {
        if (o instanceof Double) {
            return (Double) o;
        } else if (o instanceof Float) {
            return (Float) o;
        } else if (o instanceof Long || o instanceof Integer) {
            return getInt(o);
        }
        if (o == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return Double.parseDouble((String) o);
    }
}
