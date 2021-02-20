/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.commons.io;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class JSONUtils {

    public static Object loadJSONFile(final File file) throws Exception {
        final JSONParser parser = new JSONParser();
        try (FileReader fileReader = new FileReader(file)) {
            return parser.parse(fileReader);
        }
    }

    public static void writeJSON(final JSONObject json, final File file) throws Exception {
        try (FileWriter fileWriter = new FileWriter(file)) {
            final File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            fileWriter.write(json.toJSONString());
            fileWriter.flush();
        }
    }
}
