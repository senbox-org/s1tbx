package org.esa.beam.framework.datamodel;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.util.math.Rotator;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class GcpGeoCodingTest {

    @Ignore
    @Test
    public void testHowRotatedPoleTransformAffectsApproximation() {
        double[] x = new double[]{
                43.5,
                37.5,
                523.5,
                530.5,
                1075.5,
                1074.5,
                832.5,
                229.5,
                524.5
        };
        double[] y = new double[]{
                22.5,
                284.5,
                289.5,
                18.5,
                17.5,
                284.5,
                157.5,
                157.5,
                155.5
        };
        double[] lats = new double[]{
                49.27275,
                46.573524,
                45.553078,
                48.319298,
                46.770004,
                44.09146,
                46.09978,
                47.545685,
                46.92791
        };
        double[] lons = new double[]{
                6.051173,
                5.255776,
                11.624002,
                12.795015,
                19.971725,
                18.575577,
                16.156733,
                8.208557,
                12.158185
        };

        final GeoPos geoPos = GcpGeoCoding.calculateCentralGeoPos(lons, lats);

        final Rotator rotator = new Rotator(geoPos.lon, geoPos.lat);
        final double[] lons2 = Arrays.copyOf(lons, lons.length);
        final double[] lats2 = Arrays.copyOf(lats, lats.length);

        rotator.transform(lons2, lats2);

        final GcpGeoCoding.RationalFunctionMap2D forwardMap =
                new GcpGeoCoding.RationalFunctionMap2D(2, 0, x, y, lons, lats);
        final GcpGeoCoding.RationalFunctionMap2D forwardMap2 =
                new GcpGeoCoding.RationalFunctionMap2D(2, 0, x, y, lons2, lats2);

        System.out.println("forwardMap.getRmseU() = " + forwardMap.getRmseU());
        System.out.println("forwardMap.getRmseV() = " + forwardMap.getRmseV());
        System.out.println("forwardMap2.getRmseU() = " + forwardMap2.getRmseU());
        System.out.println("forwardMap2.getRmseV() = " + forwardMap2.getRmseV());

        final GcpGeoCoding.RationalFunctionMap2D inverseMap =
                new GcpGeoCoding.RationalFunctionMap2D(2, 0, lons, lats, x, y);
        final GcpGeoCoding.RationalFunctionMap2D inverseMap2 =
                new GcpGeoCoding.RationalFunctionMap2D(2, 0, lons2, lats2, x, y);

        System.out.println("inverseMap.getRmseU() = " + inverseMap.getRmseU());
        System.out.println("inverseMap.getRmseV() = " + inverseMap.getRmseV());
        System.out.println("inverseMap2.getRmseU() = " + inverseMap2.getRmseU());
        System.out.println("inverseMap2.getRmseV() = " + inverseMap2.getRmseV());

        rotator.transformInversely(lons2, lats2);

        for (int i = 0; i < lats.length; i++) {
            assertEquals(lats[i], lats2[i], 1.0E-6);
            assertEquals(lons[i], lons2[i], 1.0E-6);
        }
    }
}
