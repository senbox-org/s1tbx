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

package org.esa.beam.framework.dataop.projection;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.operation.projection.MapProjection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.List;

/**
 * Test data can be found at General Cartographic Transformation Package (GCTP).
 * It can be retrieved from: ftp://edcftp.cr.usgs.gov/pub/software/gctpc/
 */
public abstract class AbstractProjectionTest<T extends MapProjection.AbstractProvider> {

    private static final double TOLERANCE = 1.0E-4;

    private List<ProjTestData> testData;
    private T provider;

    @Before
    public void setUp() {
        testData = createTestData();
        provider = createProvider();
    }

    @After
    public void tearDown() {
        testData.clear();
        provider = null;
    }

    @Test
    public final void testMathTransform() throws TransformException {

        MathTransform transform = createMathTransform(provider);
        for (int i = 0; i < testData.size(); i++) {
            ProjTestData data = testData.get(i);
            final DirectPosition2D geo = new DirectPosition2D(data.getLon(), data.getLat());
            final DirectPosition2D map = new DirectPosition2D(data.getMapX(), data.getMapY());
            final DirectPosition2D geoInv = new DirectPosition2D(data.getLonInv(), data.getLatInv());
            doTransform(geo, map, transform, i);
            doTransformInverse(map, geoInv, transform, i);
        }
    }

    protected abstract  T createProvider();

    protected abstract MathTransform createMathTransform(T provider);

    protected abstract List<ProjTestData> createTestData();

    /*
    * Check if two coordinate points are equals, in the limits of the specified
    * tolerance vector.
    *
    * @param expected  The expected coordinate point.
    * @param actual    The actual coordinate point.
    * @param tolerance The tolerance vector. If this vector length is smaller than the number
    *                  of dimension of <code>actual</code>, then the last tolerance value will
    *                  be reused for all extra dimensions.
    */
    private static void assertPositionEquals(final DirectPosition expected,
                                             final DirectPosition actual,
                                             final double tolerance, int datasetIndex) {
        final int dimension = actual.getDimension();
        org.junit.Assert.assertEquals("The coordinate point doesn't have the expected dimension",
                                      expected.getDimension(), dimension);
        for (int i = 0; i < dimension; i++) {
            final String message = String.format("Mismatch for ordinate %d (zero-based) of dataset %d:", i, datasetIndex);
            org.junit.Assert.assertEquals(message,
                                          expected.getOrdinate(i), actual.getOrdinate(i),
                                          tolerance);
        }
    }

    /*
    * Helper method to test transform from a source to a target point.
    * Coordinate points are (x,y) or (long, lat)
    */
    private static void doTransform(DirectPosition source, DirectPosition target,
                                    MathTransform transform, final int datasetIndex) throws TransformException {
        DirectPosition calculated = transform.transform(source, null);
        assertPositionEquals(target, calculated, TOLERANCE, datasetIndex);
    }

    private static void doTransformInverse(DirectPosition source, DirectPosition target,
                                           MathTransform transform, final int datasetIndex) throws TransformException {
        DirectPosition calculated = transform.inverse().transform(source, null);
        assertPositionEquals(target, calculated, TOLERANCE, datasetIndex);
    }
}