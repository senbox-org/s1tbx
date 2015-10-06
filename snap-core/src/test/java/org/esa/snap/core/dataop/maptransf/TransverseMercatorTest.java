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
package org.esa.snap.core.dataop.maptransf;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.param.Parameter;

import java.awt.geom.Point2D;

public class TransverseMercatorTest extends TestCase {

    private double _geodeticDelta = 0.001; // accuracy = 0.001 degree
    private double _metricDelta = 0.01;    // accuracy = 0.01 meter = 1 centimeter

    private double[][] _srcCoords = {{-60.0, -10.0}, // !! is in LON/LAT order!!!!
                                     {-50.0, -9.0},
                                     {-40.0, -8.0},
                                     {-30.0, -7.0},
                                     {-20.0, -6.0},
                                     {-10.0, -5.0},
                                     {0.0, -4.0},
                                     {10.0, -3.0},
                                     {20.0, -2.0},
                                     {30.0, -1.0},
                                     {40.0, 0.0},
                                     {50.0, 1.0},
                                     {60.0, 2.0}
    };

    // As the unit is meters, the max error in this value set is 26 cm
    private double[][] _targCoords = {{-8068822.8, -2130949.9},
                                      {-6305109.9, -1532707.95},
                                      {-4801018.9, -1151790.64},
                                      {-3473214.54, -893293.22},
                                      {-2259908.81, -706020.86},
                                      {-1114611.36, -561428.87},
                                      {0.0, -442304.31},
                                      {1117373.85, -336868.94},
                                      {2271863.01, -235434.52},
                                      {3504051.59, -127815.53},
                                      {4868066.39, 0.0},
                                      {6442239.64, 172127.59},
                                      {8343966.24, 435940.11}
    };
//
//    // These are the correct values. Due to rounding
//    // from double to float, the values have to be corrected.
//    // As the unit is meters, the max error in this value set is
//    // 26 cm
//private double[][] _targCoords = {{-8068822.80388, -2130950.0},     //{-8068822.79, -2130949.9},
//  {-6305110.0, -1532708.0},     //{-6305109.9, -1532707.95},
//  {-4801019.0, -1151790.625},   //{-4801018.9, -1151790.64},
//  {-3473214.5, -893293.1875},    //{-3473214.54, -893293.22},
//  {-2259908.75, -706020.875},   //{-2259908.81, -706020.86},
//  {-1114611.375, -561428.875},  //{-1114611.36, -561428.87},
//  {0.0, -442304.28125},          // {0.0, -442304.31},
//  {1117373.875, -336868.9375},  //{1117373.85, -336868.94},
//  {2271863.0, -235434.515625},  //{2271863.01, -235434.52},
//  {3504051.75, -127815.5234375},  //{3504051.59, -127815.53},
//  {4868066.5, 0.0},             //{4868066.39, 0.0},
//  {6442240.0, 172127.59375},    //{6442239.64, 172127.59},
//  {8343966.5, 435940.125}       //{8343966.24, 435940.11}
//};
    public TransverseMercatorTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TransverseMercatorTest.class);
    }

    public void testDescriptor() {
        MapTransformDescriptor desc = new TransverseMercatorDescriptor();
        Parameter[] parameter = null;

        assertEquals("Transverse_Mercator", desc.getTypeID());
        assertEquals("meter", desc.getMapUnit());
        parameter = desc.getParameters();
        assertNotNull(parameter);
    }

    public void testForwardTransform() {
        double[] params = new double[]{
                Ellipsoid.WGS_84.getSemiMajor(),
                                       Ellipsoid.WGS_84.getSemiMinor(),
                                       0.0,
                                       0.0,
                                       1.0,
                                       0.0,
                                       0.0};
        MapTransformDescriptor desc = new TransverseMercatorDescriptor();
        MapTransform trans = desc.createTransform(params);

        GeoPos geoPt = new GeoPos();
        Point2D ptRet = null;

        for (int n = 0; n < _srcCoords.length; n++) {
            geoPt.lat = (float) _srcCoords[n][1];
            geoPt.lon = (float) _srcCoords[n][0];
            ptRet = trans.forward(geoPt, ptRet);

            assertEquals(_targCoords[n][0], ptRet.getX(), _metricDelta);
            assertEquals(_targCoords[n][1], ptRet.getY(), _metricDelta);
        }
    }

    public void testForwarAndInverseTransform() {
        // values for this test are from Frank Fell!
        double[] params = new double[]{Ellipsoid.WGS_84.getSemiMajor(),
                                       Ellipsoid.WGS_84.getSemiMinor(),
                                       0.0,
                                       9.0,
                                       0.9996,
                                       500000.0,
                                       0.0};

        MapTransform transform = MapTransformFactory.createTransform("Transverse_Mercator", params);

        float lat = 53.786666666f, lon = 11.87305555f;
        GeoPos geoPos = new GeoPos(lat, lon);

        Point2D mapPoint = transform.forward(geoPos, null);
        assertEquals(689265.13, mapPoint.getX(), _metricDelta);
        assertEquals(5963616.5, mapPoint.getY(), _metricDelta);

        GeoPos testPos = transform.inverse(mapPoint, null);
        assertEquals(lon, testPos.getLon(), _geodeticDelta);
        assertEquals(lat, testPos.getLat(), _geodeticDelta);
    }


    public void testCartographicMapTransformParameterSettings() {
        double[] params = new double[]{Ellipsoid.WGS_84.getSemiMajor(),
                                       Ellipsoid.WGS_84.getSemiMinor(),
                                       1.0,
                                       2.0,
                                       3.0,
                                       4.0,
                                       5.0};

        MapTransform mt = MapTransformFactory.createTransform("Transverse_Mercator", params);
        assertTrue(mt instanceof CartographicMapTransform);

        CartographicMapTransform amt = (CartographicMapTransform) mt;
        assertEquals(2.0, amt.getCentralMeridian(), 1e-10);
        assertEquals(4.0, amt.getFalseEasting(), 1e-10);
        assertEquals(5.0, amt.getFalseNorthing(), 1e-10);
        assertEquals(1.0 * Ellipsoid.WGS_84.getSemiMajor(), amt.getSemiMajor(), 1e-10);
        assertEquals(1.0 / Ellipsoid.WGS_84.getSemiMajor(), amt.getInverseSemiMajor(), 1e-10);
    }
}
