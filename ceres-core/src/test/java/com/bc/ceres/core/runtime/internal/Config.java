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

package com.bc.ceres.core.runtime.internal;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;


public class Config {

    private static final File TEST_DIR;
    private static final String TEST_DIRNAME = "testdirs";

    static {
        File testDir = null;
        URL resource = Config.class.getResource("/" + TEST_DIRNAME);
        if (resource != null) {
            try {
                testDir = new File(resource.toURI());
            } catch (URISyntaxException e) {
                // ok
            }
        }
        if (testDir == null) {
            testDir = new File("target/test-classes/" + TEST_DIRNAME).getAbsoluteFile();
        }
        TEST_DIR = testDir;
    }

    public static File getDirForAppA() {
        return new File(TEST_DIR, "app-a");
    }

    public static File getDirForAppB() {
        return new File(TEST_DIR, "app-b");
    }

    public static File getDirForAppC() {
        return new File(TEST_DIR, "app-c");
    }

    public static File getRepositoryDir() {
        return new File(TEST_DIR, "repository");
    }
}
