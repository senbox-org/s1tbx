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
public class LandsatGeotiffReaderPluginTest {

    private LandsatGeotiffReaderPlugin plugin;

    @Before
    public void setUp() throws Exception {
        plugin = new LandsatGeotiffReaderPlugin();
    }

    @Test
    public void testIsLandsatMSSFilename() throws Exception {
        assertTrue(LandsatGeotiffReaderPlugin.isLandsatMSSFilename("LM11870291976166ESA00_MTL.txt"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsatMSSFilename("LM32170241982254XXX01_MTL.txt"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsatMSSFilename("LM42310081982267ESA00_MTL.txt"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsatMSSFilename("LM52010241984295AAA03_MTL.txt"));

        assertFalse(LandsatGeotiffReaderPlugin.isLandsatMSSFilename("LT40140341983030XXX13_MTL.txt"));
    }

    @Test
    public void testIsLandsat4Filename() throws Exception {
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat4Filename("LT40140341983030XXX13_MTL.txt"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat4Filename("LT40140341982315PAC00_MTL.txt"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat4Filename("LT41160361989137XXX02_MTL.txt"));

        assertFalse(LandsatGeotiffReaderPlugin.isLandsat4Filename("LT40140341982315PAC00_B1.TIF"));
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat4Filename("LT51920342011274MPS00.tgz"));
    }

    @Test
    public void testIsLandsat5Filename() throws Exception {
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat5Filename("LT51231232013068GSI01_MTL.txt"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat5Filename("LT51231232013068GSI01_MTL.TXT"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat5Filename("LT51920342011274MPS00.tar.gz"));

        assertFalse(LandsatGeotiffReaderPlugin.isLandsat5Filename("LT51231232013068GSI01_B3.txt")); // is a band name, not the metadata file
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat5Filename("L5196030_03020031023_MTL.txt"));  // Sensor type missing
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat5Filename("LT71920342011274MPS00.tar.gz")); // '5' expected after 'LT'
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat5Filename("LT51920342011274MPS00.tgz")); // 'tar.gz' expected as extension
    }

    @Test
    public void testIsLandsat7Filename() throws Exception {
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat7Filename("LE71890342011277ASN00_MTL.txt"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat7Filename("LE71890342011277ASN00_MTL.TXT"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat7Filename("LE71890342011277ASN00.tar.gz"));

        assertFalse(LandsatGeotiffReaderPlugin.isLandsat7Filename("LE71890342011277ASN00_B3.txt")); // is a band name, not the metadata file
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat7Filename("L71890342011277ASN00.txt"));  // Sensor type missing
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat7Filename("LE81890342011277ASN00_MTL.txt")); // '7' expected after 'LT'
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat7Filename("LE71890342011277ASN00.tgz")); // 'tar.gz' expected as extension
    }

    @Test
    public void testIsLandsat8Filename() throws Exception {
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat8Filename("LT82270322013068LGN01_MTL.txt"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat8Filename("LT82270322013068LGN01_MTL.TXT"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat8Filename("LC82160332013191LGN00.tar.gz"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat8Filename("LO82160332013191LGN00.tar.gz"));
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat8Filename("LT82160332013191LGN00.tar.gz"));

        assertFalse(LandsatGeotiffReaderPlugin.isLandsat8Filename("L8196030_03020031023_MTL.txt"));  // Sensor type missing
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat8Filename("LT52160332013191LGN00.tar.gz")); // '8' expected after 'LT'
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat8Filename("LT82160332013191LGN00.tgz")); // 'tar.gz' expected as extension
    }

    @Test
    public void testIsLandsat5LegacyFilename() throws Exception {
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat5LegacyFilename("LT51960300302003GSI01_MTL.txt")); //according to specification
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat5LegacyFilename("LT51960300302003GSI01_MTL.TXT"));

        assertTrue(LandsatGeotiffReaderPlugin.isLandsat5LegacyFilename("L5196030_03020031023_MTL.txt")); //according to real-world data
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat5LegacyFilename("L5196030_03020031023_MTL.TXT"));

        assertTrue(LandsatGeotiffReaderPlugin.isLandsat5LegacyFilename("LT51960302003296MTI01.tar.gz"));

        assertFalse(LandsatGeotiffReaderPlugin.isLandsat5LegacyFilename("L51950302003257MTI01.tar.gz"));  // Sensor type missing
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat5LegacyFilename("LT72160332013191LGN00.tar.gz")); // '5' expected after 'LT'
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat5LegacyFilename("LT82160332013191LGN00.tgz")); // 'tar.gz' or 'txt' expected as extension
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat5LegacyFilename("LT82160332013191LGN00.dat")); // 'tar.gz' or 'txt' expected as extension
    }

    @Test
    public void testIsLandsat7LegacyFilename() throws Exception {
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat7LegacyFilename("LE71960300302003GSI01_MTL.txt")); //according to specification
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat7LegacyFilename("LE71960300302003GSI01_MTL.TXT"));

        assertTrue(LandsatGeotiffReaderPlugin.isLandsat7LegacyFilename("L71196030_03020031023_MTL.txt")); //according to real-world data
        assertTrue(LandsatGeotiffReaderPlugin.isLandsat7LegacyFilename("L71196030_03020031023_MTL.TXT"));

        assertTrue(LandsatGeotiffReaderPlugin.isLandsat7LegacyFilename("LE71960302003296ASN01.tar.gz"));

        assertFalse(LandsatGeotiffReaderPlugin.isLandsat7LegacyFilename("L71950302003257MTI01.tar.gz"));  // Sensor type missing
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat7LegacyFilename("LE52160332013191LGN00.tar.gz")); // '7' expected after 'LT'
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat7LegacyFilename("LE72160332013191LGN00.tgz")); // 'tar.gz' or 'txt' expected as extension
        assertFalse(LandsatGeotiffReaderPlugin.isLandsat7LegacyFilename("LE72160332013191LGN00.dat")); // 'tar.gz' or 'txt' expected as extension
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
    }

    @Test
    public void testGetFormatNames() throws Exception {
        assertArrayEquals(new String[]{"LandsatGeoTIFF"}, plugin.getFormatNames());
    }

    @Test
    public void testGetDefaultFileExtensions() throws Exception {
        assertArrayEquals(new String[]{".txt", ".TXT", ".gz"}, plugin.getDefaultFileExtensions());
    }

    @Test
    public void testGetDescription() throws Exception {
        assertEquals("Landsat Data Products (GeoTIFF)", plugin.getDescription(null));
    }

    @Test
    public void testGetProductFileFilter() throws Exception {
        assertNotNull(plugin.getProductFileFilter());
    }

    @Test
    public void testIsMetadataFile() throws Exception {
        File positiveFile1 = new File(getClass().getResource("test_L8_MTL.txt").getFile());
        assertTrue(LandsatGeotiffReaderPlugin.isMetadataFile(positiveFile1));
        File positiveFile2 = new File(getClass().getResource("test_legacy_L5_WithTrailingWhiteSpace_MTL.txt").getFile());
        assertTrue(LandsatGeotiffReaderPlugin.isMetadataFile(positiveFile2));
        File negativeFile = new File(getClass().getResource("test_MTL_L7.txt").getFile());
        assertFalse(LandsatGeotiffReaderPlugin.isMetadataFile(negativeFile));

    }
}
