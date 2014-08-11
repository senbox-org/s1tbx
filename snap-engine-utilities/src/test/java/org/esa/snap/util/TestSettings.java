/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.util;

import junit.framework.TestCase;

/**
 * Settings Tester.
 *
 * @author lveci
 */
public class TestSettings extends TestCase {

    public TestSettings(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testLoadSettings() {
        final Settings settings = Settings.instance();

        String value1 = settings.get("AuxData/envisatAuxDataPath");

        String value2 = settings.get("DEM/srtm3GeoTiffDEM_FTP");
    }

    public void testGet() {
        final Settings settings = Settings.instance();

        String value = settings.get("DEM/srtm3GeoTiffDEM_FTP");
    }
}