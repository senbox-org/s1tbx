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

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.io.FileReader;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class Landsat8MetadataTest {

    private Landsat8Metadata metadata;

    @Before
    public void setUp() throws Exception {
        metadata = new Landsat8Metadata(new FileReader(new File(getClass().getResource("test_L8_MTL.txt").getFile())));
    }

    @Test
    public void testGetCenterTime() throws Exception {
        ProductData.UTC expected = ProductData.UTC.parse("2013-04-12 10:55:46.338", "yyyy-MM-dd HH:mm:ss");
        assertEquals(expected.getAsDate().getTime(), metadata.getCenterTime().getAsDate().getTime());
    }

    @Test
    public void testDimensions() throws Exception {
        assertEquals(new Dimension(15301, 14901), metadata.getPanchromaticDim());
        assertEquals(new Dimension(7651, 7451), metadata.getReflectanceDim());
        assertEquals(new Dimension(7651, 7451), metadata.getThermalDim());
    }

    @Test
    public void testGetProductType() throws Exception {
        assertEquals("LANDSAT_8_OLI_TIRS_L1GT", metadata.getProductType());
    }

    @Test
    public void testGetScalingFactor() throws Exception {
        assertEquals(0.0128377, metadata.getScalingFactor("1"), 1E-7);
        assertEquals(0.0003342, metadata.getScalingFactor("11"), 1E-7);
        assertEquals(1, metadata.getScalingFactor("12"), 1E-5);
    }

    @Test
    public void testGetScalingOffset() throws Exception {
        assertEquals(-64.18854770, metadata.getScalingOffset("1"), 1E-7);
        assertEquals(0.09999579, metadata.getScalingOffset("11"), 1E-7);
        assertEquals(0, metadata.getScalingOffset("12"), 1E-7);
    }
}
