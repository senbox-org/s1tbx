/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.landsat.geotiff;

import org.esa.beam.framework.dataio.ProductReader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class Landsat8GeotiffReaderPluginTest {

    private Landsat8GeotiffReaderPlugin plugin;

    @Before
    public void setUp() throws Exception {
        plugin = new Landsat8GeotiffReaderPlugin();
    }

    @Test
    public void testIsLandsat8Filename() throws Exception {
        assertTrue(Landsat8GeotiffReaderPlugin.isLandsat8Filename("LT82270322013068LGN01_MTL.txt"));
        assertTrue(Landsat8GeotiffReaderPlugin.isLandsat8Filename("LC82160332013191LGN00.tar.gz"));
        assertTrue(Landsat8GeotiffReaderPlugin.isLandsat8Filename("LO82160332013191LGN00.tar.gz"));
        assertTrue(Landsat8GeotiffReaderPlugin.isLandsat8Filename("LT82160332013191LGN00.tar.gz"));

        assertFalse(Landsat8GeotiffReaderPlugin.isLandsat8Filename("L5196030_03020031023_MTL.txt"));  // Sensor type missing
        assertFalse(Landsat8GeotiffReaderPlugin.isLandsat8Filename("LT52160332013191LGN00.tar.gz")); // '8' expected after 'LT'
        assertFalse(Landsat8GeotiffReaderPlugin.isLandsat8Filename("LT82160332013191LGN00.tgz")); // 'tar.gz' expected as extension

    }

    @Test
    public void testGetInputTypes() throws Exception {
        assertArrayEquals(new Class[]{String.class, File.class}, plugin.getInputTypes());
    }

    @Test
    public void testCreateReaderInstance() throws Exception {
        ProductReader productReader = plugin.createReaderInstance();
        assertNotNull(productReader);
        assertTrue(productReader instanceof LandsatGeotiffReader);
        assertTrue(((LandsatGeotiffReader) productReader).landsatMetadataFactory instanceof LandsatMetadataFactory.Landsat8MetadataFactory);
    }

    @Test
    public void testGetFormatNames() throws Exception {
        assertArrayEquals(new String[]{"Landsat8GeoTIFF"}, plugin.getFormatNames());
    }

    @Test
    public void testGetDefaultFileExtensions() throws Exception {
        assertArrayEquals(new String[]{".txt", ".TXT", ".gz"}, plugin.getDefaultFileExtensions());
    }

    @Test
    public void testGetDescription() throws Exception {
        assertEquals("Landsat 8 Data Products (GeoTIFF)", plugin.getDescription(null));
    }

    @Test
    public void testGetProductFileFilter() throws Exception {
        assertNotNull(plugin.getProductFileFilter());
    }

    @Test
    public void testIsMetadataFile() throws Exception {
        File positiveFile = new File(getClass().getResource("test_L8_MTL.txt").getFile());
        assertTrue(org.esa.beam.dataio.landsat.geotiff.Landsat8GeotiffReaderPlugin.isMetadataFile(positiveFile));
        File negativeFile = new File(getClass().getResource("test_MTL_L7.txt").getFile());
        assertFalse(org.esa.beam.dataio.landsat.geotiff.Landsat8GeotiffReaderPlugin.isMetadataFile(negativeFile));

    }
}
