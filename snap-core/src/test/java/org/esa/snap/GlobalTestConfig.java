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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.util.SystemUtils;

import java.io.File;

public class GlobalTestConfig {

    public static final String BEAM_TEST_DATA_INPUT_DIR_PROPERTY_NAME = "org.esa.snap.testdata.in";
    public static final String BEAM_TEST_DATA_OUTPUT_DIR_PROPERTY_NAME = "org.esa.snap.testdata.out";
    public static final String BEAM_TEST_DATA_INPUT_DIR_DEFAULT_PATH = "testdata" + File.separatorChar + "in";
    public static final String BEAM_TEST_DATA_OUTPUT_DIR_DEFAULT_PATH = "testdata" + File.separatorChar + "out";

    public static File getBeamTestDataInputDirectory() {
        return getDirectory(BEAM_TEST_DATA_INPUT_DIR_PROPERTY_NAME,
                            BEAM_TEST_DATA_INPUT_DIR_DEFAULT_PATH);
    }

    public static File getBeamTestDataInputFile(String relPath) {
        return new File(getBeamTestDataInputDirectory(),
                        SystemUtils.convertToLocalPath(relPath));
    }

    public static File getBeamTestDataOutputDirectory() {
        return getDirectory(BEAM_TEST_DATA_OUTPUT_DIR_PROPERTY_NAME,
                            BEAM_TEST_DATA_OUTPUT_DIR_DEFAULT_PATH);
    }

    public static File getBeamTestDataOutputFile(String relPath) {
        return new File(getBeamTestDataOutputDirectory(),
                        SystemUtils.convertToLocalPath(relPath));
    }

    public static Test createTest(final Class testClass, final File inputFile) {
        if (!inputFile.exists()) {
            return createEmptyTest(testClass, inputFile);
        } else {
            return new TestSuite(testClass);
        }
    }

    private static Test createEmptyTest(final Class testClass, final File inputFile) {
        return new TestCase(testClass.getName()) {

            @Override
            public void runTest() {
                System.out.println();
                System.out.println(testClass.getName() + ": warning: test will not be performed: testdata not found: ");
                System.out.println(inputFile.getPath());
            }
        };
    }

    private static File getDirectory(String propertyName, String beamRelDefaultPath) {
        String filePath = System.getProperty(propertyName);
        if (filePath != null) {
            return new File(filePath);
        }
        return new File(SystemUtils.getApplicationHomeDir(),
                        SystemUtils.convertToLocalPath(beamRelDefaultPath));
    }
}
