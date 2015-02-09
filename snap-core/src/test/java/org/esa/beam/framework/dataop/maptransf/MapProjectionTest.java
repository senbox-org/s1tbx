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

package org.esa.beam.framework.dataop.maptransf;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;

import org.esa.beam.GlobalTestConfig;
import org.esa.beam.TestNotExecutableException;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;

public class MapProjectionTest extends TestCase {

    final static File MERIS_TEST_FILE = GlobalTestConfig.getBeamTestDataInputFile(
            "Envisat/data/MERIS/L1b/MER_RR__1PNPDK20020421_104345_000002702005_00180_00735_1618.N1");
    //final static File MERIS_TEST_FILE = new File("e:/data/MERIS/L1B/MER_RR__1P_Italy.N1");

    public MapProjectionTest(String s) {
        super(s);
    }

    public static Test suite() {
        return GlobalTestConfig.createTest(MapProjectionTest.class,
                                           MERIS_TEST_FILE);
//        return new TestSuite(MapProjectionTest.class);
    }

    public void testOutputRasterSize() {
        Product product = null;
        try {
            product = ProductIO.readProduct(MERIS_TEST_FILE);
        } catch (IOException e) {
            throw new TestNotExecutableException(e);
        }

        MapTransform mapTransform = MapTransformFactory.createTransform("Identity", null);
        Dimension size = ProductUtils.getOutputRasterSize(product, null, mapTransform, 0.05F, 0.05F);

        try {
            product.closeProductReader();
        } catch (IOException e) {
            Debug.trace(e);
        }

        assertEquals(364, size.width);
        assertEquals(366, size.height);

        // transverse mercator
        double[] params = new double[]{
            Ellipsoid.WGS_84.getSemiMajor(),
            Ellipsoid.WGS_84.getSemiMajor(),
            0.0,
            0.0,
            0.9996,
            0.0,
            0.0
        };
        mapTransform = MapTransformFactory.createTransform("Transverse_Mercator", params);
        size = ProductUtils.getOutputRasterSize(product, null, mapTransform, 0.05, 0.05);

        try {
            product.closeProductReader();
        } catch (IOException e) {
            Debug.trace(e);
        }

        assertEquals(32950009, size.width);
        assertEquals(41530525, size.height);
    }

    public void testX() {

        final TransverseMercatorDescriptor tmd = new TransverseMercatorDescriptor();
        final MapProjection mp = new MapProjection("bibo", tmd.createTransform(null), "meter");
        assertEquals(Ellipsoid.WGS_84.getSemiMajor(), mp.getMapTransform().getParameterValues()[0], 1e-5);
        assertEquals(Ellipsoid.WGS_84.getSemiMinor(), mp.getMapTransform().getParameterValues()[1], 1e-5);
        mp.alterMapTransform(Ellipsoid.BESSEL);
        assertEquals(Ellipsoid.BESSEL.getSemiMajor(), mp.getMapTransform().getParameterValues()[0], 1e-5);
        assertEquals(Ellipsoid.BESSEL.getSemiMinor(), mp.getMapTransform().getParameterValues()[1], 1e-5);
    }

}
