/*
 * $Id: GlobalTestConfig.java,v 1.3 2006/10/10 14:47:30 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.util.SystemUtils;

import java.io.File;

public class GlobalTestConfig {

    public static final String BEAM_TEST_DATA_INPUT_DIR_PROPERTY_NAME = "org.esa.beam.testdata.in";
    public static final String BEAM_TEST_DATA_OUTPUT_DIR_PROPERTY_NAME = "org.esa.beam.testdata.out";
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
        return new File(SystemUtils.getBeamHomeDir(),
                        SystemUtils.convertToLocalPath(beamRelDefaultPath));
    }
}
