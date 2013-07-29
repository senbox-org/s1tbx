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

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class LandsatMetadataFactoryTest {

    @Test
    public void testCreate_5_reproc() throws Exception {
        File testFile = new File(getClass().getResource("test_5_reproc_MTL.txt").getFile());
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof LandsatReprocessedMetadata);
    }

    @Test
    public void testCreate_7_reproc() throws Exception {
        File testFile = new File(getClass().getResource("test_7_reproc_MTL.txt").getFile());
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof LandsatReprocessedMetadata);
    }

    @Test
    public void testCreate_8() throws Exception {
        File testFile = new File(getClass().getResource("test_L8_MTL.txt").getFile());
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof Landsat8Metadata);
    }

    @Test
    public void testCreate_Legacy() throws Exception {
        File testFile = new File(getClass().getResource("test_L7_MTL.txt").getFile());
        LandsatMetadata landsatMetadata = LandsatMetadataFactory.create(testFile);
        assertTrue(landsatMetadata instanceof LandsatLegacyMetadata);
    }

    @Test(expected = IllegalStateException.class)
    public void testFail() throws IOException {
        File testFile = new File(getClass().getResource("test_broken_MTL.txt").getFile());
        LandsatMetadataFactory.create(testFile);
    }
}
