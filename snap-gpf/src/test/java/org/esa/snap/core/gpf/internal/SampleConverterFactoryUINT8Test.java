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

package org.esa.snap.core.gpf.internal;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class SampleConverterFactoryUINT8Test {

    private static Band band;

    @Before
    public void beforeTest() {
        band = new Band("n", ProductData.TYPE_UINT8, 1, 1);
        band.setRasterData(band.createCompatibleRasterData());
    }


    @Test
    public void testToGeoPhysical_Unscaled() {
        SampleConverterFactory.SampleConverter sampleConverter = SampleConverterFactory.createConverter(band);
        assertNotNull(sampleConverter);

        assertEquals(255, sampleConverter.toGeoPhysical(255), 1.0e-6);
        assertEquals(0, sampleConverter.toGeoPhysical(0), 1.0e-6);
        assertEquals(16.0, sampleConverter.toGeoPhysical(16.0), 1.0e-6);
        assertEquals(128, sampleConverter.toGeoPhysical(128), 1.0e-6);
    }

    @Test
    public void testToGeoPhysical_Scaled() {
        double scalingFactor = 1.1;
        band.setScalingFactor(scalingFactor);

        SampleConverterFactory.SampleConverter sampleConverter = SampleConverterFactory.createConverter(band);
        assertNotNull(sampleConverter);

        assertEquals(255 * scalingFactor, sampleConverter.toGeoPhysical(255), 1.0e-6);
        assertEquals(0 * scalingFactor, sampleConverter.toGeoPhysical(0), 1.0e-6);
        assertEquals(16.0 * scalingFactor, sampleConverter.toGeoPhysical(16.0), 1.0e-6);
        assertEquals(128 * scalingFactor, sampleConverter.toGeoPhysical(128), 1.0e-6);
    }


    @Test
    public void testToRaw_Unscaled() {
        SampleConverterFactory.SampleConverter sampleConverter = SampleConverterFactory.createConverter(band);
        assertNotNull(sampleConverter);

        assertEquals(255, sampleConverter.toRaw(255), 1.0e-6);
        assertEquals(0, sampleConverter.toRaw(0), 1.0e-6);
        assertEquals(10.8, sampleConverter.toRaw(10.8), 1.0e-6);
        assertEquals(128, sampleConverter.toRaw(128), 1.0e-6);
        // the converted shall be within the raw value range and no overflow shall appear
        assertEquals(0, sampleConverter.toRaw(-1), 1.0e-6);
        assertEquals(255, sampleConverter.toRaw(256), 1.0e-6);
    }

    @Test
    public void testToRaw_Scaled() {
        double scalingFactor = 1.1;
        band.setScalingFactor(scalingFactor);

        SampleConverterFactory.SampleConverter sampleConverter = SampleConverterFactory.createConverter(band);
        assertNotNull(sampleConverter);

        assertEquals(255 / 1.1, sampleConverter.toRaw(255), 1.0e-6);
        assertEquals(0 / 1.1, sampleConverter.toRaw(0), 1.0e-6);
        assertEquals(10.8 / 1.1, sampleConverter.toRaw(10.8), 1.0e-6);
        assertEquals(128 / 1.1, sampleConverter.toRaw(128), 1.0e-6);
        // the converted shall be within the raw value range and no overflow shall appear
        assertEquals(0, sampleConverter.toRaw(-1 * 1.1), 1.0e-6);
        assertEquals(255, sampleConverter.toRaw(256 * 1.1), 1.0e-6);
    }

}
