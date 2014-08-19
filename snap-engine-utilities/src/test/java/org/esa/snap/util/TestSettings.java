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

import org.junit.Test;

/**
 * Settings Tester.
 *
 * @author lveci
 */
public class TestSettings {

    @Test
    public void testLoadSettings() {
        final Settings settings = Settings.instance();

        String value1 = settings.get("AuxData.envisatAuxDataPath");

        String value2 = settings.get("DEM.srtm3GeoTiffDEM_FTP");
    }

    @Test
    public void testGet() {
        final Settings settings = Settings.instance();

        String value1 = settings.get("DEM.srtm3GeoTiffDEM_FTP");
        String value2 = settings.get("AuxDataPath");
        String value3 = settings.get("demPath");
    }

    @Test
    public void testGetAuxDataProperty() {
        final Settings settings = Settings.instance();

        String value = settings.getAuxdataProperty().getPropertyString("DEM.srtm3GeoTiffDEM_FTP");
        assert (!value.isEmpty());
    }

    @Test
    public void testNotFound() {
        final Settings settings = Settings.instance();

        String value = settings.getAuxdataProperty().getPropertyString("string not found");
        assert (value.isEmpty());
    }
}