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
package org.esa.snap.core.util.geotiff.codes;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class CodeClassGenerator {

    final static String[] GEOTIFF_CODE_FILENAMES = {
        "geokey.properties",
        "geo_rasters.properties",
        "geo_models.properties",
        "geo_ctrans.properties",
    };

    final static String[] EPSG_CODE_FILE_NAMES = {
        "epsg_vertcs.properties",
        "epsg_unit.properties",
        "epsg_proj.properties",
        "epsg_pm.properties",
        "epsg_pcs.properties",
        "epsg_gcs.properties",
        "epsg_ellipse.properties",
        "epsg_datum.properties",
    };

    private static final String INDENT = "    ";

    public static void main(String[] args) {
        try {
            writeJavaClassFile(GEOTIFF_CODE_FILENAMES, "_GeoTIFFCodes");
            writeJavaClassFile(EPSG_CODE_FILE_NAMES, "_EPSGCodes");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }
    }

    private static void writeJavaClassFile(final String[] fileNames, final String className) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter("./src/com/bc/geotiff/" + className + ".java"));

        writer.println("package com.bc.geotiff;");
        writer.println();

        writer.print("public class ");
        writer.print(className);
        writer.print(" extends IntMap {");
        writer.println();

        for (int i = 0; i < fileNames.length; i++) {

            String fileName = fileNames[i];

            writer.print(INDENT);
            writer.print("/* Generated from file ");
            writer.print(fileName);
            writer.print(" */");
            writer.println();

            final InputStream resourceAsStream = CodeClassGenerator.class.getResourceAsStream(fileName);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                final int pos = line.indexOf("=");
                if (pos < 0) {
                    continue;
                }

                String name = line.substring(0, pos).trim();
                String value = line.substring(pos + 1).trim();
                if (value.startsWith("$")) {
                    value = value.substring(1);
                }

                writer.print(INDENT);
                writer.print("public static final int ");
                writer.print(name);
                writer.print(" = ");
                writer.print(value);
                writer.print(";");
                writer.println();
            }
            reader.close();
        }

        writer.println();
        writer.println(INDENT + "static {");
        writer.println(INDENT + INDENT + "init(" + className + ".class.getFields());");
        writer.println(INDENT + "}");

        writer.println();
        writer.println(INDENT + "private " + className + "() {");
        writer.println(INDENT + "}");

        writer.println("}");
        writer.close();
    }

}
