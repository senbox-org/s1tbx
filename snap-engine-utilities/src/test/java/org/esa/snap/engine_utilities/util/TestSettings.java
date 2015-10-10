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
package org.esa.snap.engine_utilities.util;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.runtime.Config;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.TestCase.*;

/**
 * Settings Tester.
 *
 * @author lveci
 */
public class TestSettings {

    @Before
    public void SetUp() throws IOException {
        Path propFile = SystemUtils.getApplicationHomeDir().toPath().resolve("snap-engine").resolve("etc/snap.auxdata.properties");
        if(!Files.exists(propFile)) {
            propFile = SystemUtils.getApplicationHomeDir().toPath().resolve("../etc/snap.auxdata.properties");
        }
        if(!propFile.toFile().exists()) {
            throw new IOException("Unable to locate snap.auxdata.properties");
        }
        Config.instance(Settings.SNAP_AUXDATA).load(propFile);
    }

    @Test
    public void testLoadSettings() {
        final Settings settings = Settings.instance();

        String value1 = settings.get("AuxData.envisatAuxDataPath");

        String value2 = settings.get("DEM.srtm3GeoTiffDEM_FTP");
    }

    @Test
    public void testGet() {
        String value1 = Settings.instance().get("DEM.srtm3GeoTiffDEM_FTP");
        assertEquals("xftp.jrc.it", value1);
    }

    @Test
    public void testGetAuxDataPath() {

        String value2 = Settings.instance().get("AuxDataPath");
        assertEquals(value2, SystemUtils.getAuxDataPath().toString());
    }

    @Test
    public void testVariableReplacement() {
        String value3 = Settings.instance().get("demPath");
        assert(!value3.contains("AuxDataPath"));
    }

    @Test
    public void testGetAuxDataProperty() {
        final Settings settings = Settings.instance();

        String value = settings.get("DEM.srtm3GeoTiffDEM_FTP");
        assert (!value.isEmpty());
    }

    @Test
    public void testNotFound() {
        final Settings settings = Settings.instance();

        String value = settings.get("string not found");
        assert (value.isEmpty());
    }
}