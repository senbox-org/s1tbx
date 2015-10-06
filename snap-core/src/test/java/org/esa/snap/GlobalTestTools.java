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
package org.esa.snap;

import org.esa.snap.core.util.io.FileUtils;

import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class GlobalTestTools {

    public static void deleteTestDataOutputDirectory() {
        FileUtils.deleteTree(GlobalTestConfig.getBeamTestDataOutputDirectory());
    }

    public static void deleteTestDataInputDirectory() {
        FileUtils.deleteTree(GlobalTestConfig.getBeamTestDataInputDirectory());
    }

    public static void deleteTestDirectory() {
        deleteTestDataInputDirectory();
        deleteTestDataOutputDirectory();
    }

    /**
     * Equals the given int arrays
     *
     * @param exp the expected int array
     * @param act the actual int array
     *
     * @return A String which contains the differences between the given arrays
     */
    public static String equal(final int[] exp, final int[] act) {
        if (exp == null) {
            if (act != null) {
                return "Expected was '" + exp + "' but was '" + act + "'";
            } else {
                return "";
            }
        } else {
            final String s = "An int[] with the size " + exp.length + " was expected but the given array ";
            if (act == null) {
                return s + "is " + act;
            } else if (exp.length != act.length) {
                return s + "length is " + act.length;
            } else {
                for (int i = 0; i < exp.length; i++) {
                    if (exp[i] != act[i]) {
                        return "Fail at index " + i + ": Expected <" + exp[i] + "> but was <" + act[i] + ">";
                    }
                }
            }
        }
        return "";
    }

    public static String createPlatformIndependentFilePath(String path) {
        if (path.indexOf('/') > 0) {
            path = path.replace('/', File.separatorChar);
        } else if (path.indexOf('\\') > 0) {
            path = path.replace('\\', File.separatorChar);
        }
        return path;
    }

    public static byte[] createBytes(int size) {
        final byte[] bytes = new byte[size];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i % 256);
        }
        return bytes;
    }

    public static void writeBlanks(final ImageOutputStream ios, final int numBlanks) throws IOException {
        final char[] chars = new char[numBlanks];
        Arrays.fill(chars, ' ');
        ios.writeBytes(new String(chars));
    }
}
