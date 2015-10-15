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

public class LambertConformalConicTest extends TestCase {

    // PARAMETER SET 1 --------------------------------------------------------
    // ------------------------------------------------------------------------
    // +proj=lcc +ellps=WGS84 +lat_0=90n +lat_1=20n +lat_2=60n +lon_0=0
    private static final double[] _params_1 = {
            Ellipsoid.WGS_84.getSemiMajor(), // semi major
                                               Ellipsoid.WGS_84.getSemiMinor(), // semi minor
                                               90.0, // latitude of origin
                                               0.0, // central meridian
                                               20.0, // latitude intersect 1
                                               60.0, // latitude intersect 2
                                               1.0    // scale factor
    };
    // lon      lat     x           y
    // 0.0      -4.0    0.00        -12053272.30
    // -106.0   78      -2468002.14 -915621.60
    // 54       -12     7670898.95  -10762276.17
    private static final double[][] _srcCoords_1 = {{0.0, -4.0}, // in LON, LAT pairs!
                                                    {-106.0, 78.0},
                                                    {54.0, -12.0}
    };
    // These are output values of proj, the difference to the test values
    // used for checking is below 2cm for this set
//    private static final double[][] _targCoords_1 = {{0.0, -12053272.30},
//                                                     {-2468002.14, -915621.60},
//                                                     {7670898.95, -10762276.17}
//    };
    private static final double[][] _targCoords_1 = {{0.0, -12053272.2905},
                                                     {-2468002.1476, -915621.5974},
                                                     {7670898.9414, -10762276.1508}
    };

    // PARAMETER SET 2 --------------------------------------------------------
    // ------------------------------------------------------------------------
    // +proj=lcc +ellps=WGS84 +lat_0=45s +lat_1=20n +lat_2=60n +lon_0=0
    private static final double[] _params_2 = {Ellipsoid.WGS_84.getSemiMajor(), // semi major
                                               Ellipsoid.WGS_84.getSemiMinor(), // semi minor
                                               -45.0, // latitude of origin
                                               0.0, // central meridian
                                               20.0, // latitude intersect 1
                                               60.0, // latitude intersect 2
                                               1.0    // scale factor
    };
    // lon      lat     x           y
    // -20.0    14.0    -2228513.87 10939595.66
    // 109.0    -78     47808529.90 4596804.32
    // 78       -67     25463712.96 47458.39
    private static final double[][] _srcCoords_2 = {{-20.0, 14.0}, // in LON, LAT pairs!
                                                    {109.0, -78.0},
                                                    {78.0, -67.0}
    };
    // These are output values of proj, the difference to the test values
    // used for checking is below 20cm for this set
//    private static final double[][] _targCoords_2 = {{-2228513.87, 10939595.66},
//                                                     {47808529.90, 4596804.32},
//                                                     {25463712.96, 47458.39}
//    };
    private static final double[][] _targCoords_2 = {{-2228513.8718, 10939595.6047},
                                                     {47808529.7390, 4596804.3267},
                                                     {25463712.8804, 47458.4076}
    };


    private static final double _metricDelta = 1e-4;
    private static final double _angleDelta = 1e-5;

    public LambertConformalConicTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(LambertConformalConicTest.class);
    }

    public void testDescriptor() {
        MapTransformDescriptor desc = new LambertConformalConicDescriptor();
        Parameter[] parameter = null;

        assertEquals("Lambert_Conformal_Conic", desc.getTypeID());
        assertEquals("meter", desc.getMapUnit());
        parameter = desc.getParameters();
        assertNotNull(parameter);
    }


    public void testForwardTransform() {
        GeoPos geoPt = new GeoPos();
        Point2D ptRet = null;
        MapTransformDescriptor desc = new LambertConformalConicDescriptor();

        // Parameter set 1
        // ---------------
        MapTransform trans = desc.createTransform(_params_1);
        for (int n = 0; n < _srcCoords_1.length; n++) {
            geoPt.lat = (float) _srcCoords_1[n][1];
            geoPt.lon = (float) _srcCoords_1[n][0];
            ptRet = trans.forward(geoPt, ptRet);

            assertEquals(_targCoords_1[n][0], ptRet.getX(), _metricDelta);
            assertEquals(_targCoords_1[n][1], ptRet.getY(), _metricDelta);
        }

        // Parameter set 2
        // ---------------
        trans = desc.createTransform(_params_2);
        for (int n = 0; n < _srcCoords_2.length; n++) {
            geoPt.lat = (float) _srcCoords_2[n][1];
            geoPt.lon = (float) _srcCoords_2[n][0];
            ptRet = trans.forward(geoPt, ptRet);

            assertEquals(_targCoords_2[n][0], ptRet.getX(), _metricDelta);
            assertEquals(_targCoords_2[n][1], ptRet.getY(), _metricDelta);
        }
    }

    public void testInverseTransform() {
        GeoPos geoPt = new GeoPos();
        Point2D mapPt = new Point2D.Double();
        MapTransformDescriptor desc = new LambertConformalConicDescriptor();

        // Parameter set 1
        // ---------------
        MapTransform trans = desc.createTransform(_params_1);
        for (int n = 0; n < _targCoords_1.length; n++) {
            mapPt.setLocation(_targCoords_1[n][0], _targCoords_1[n][1]);
            geoPt = trans.inverse(mapPt, geoPt);

            assertEquals(_srcCoords_1[n][1], geoPt.getLat(), _angleDelta);
            assertEquals(_srcCoords_1[n][0], geoPt.getLon(), _angleDelta);
        }

        // Parameter set 2
        // ---------------
        trans = desc.createTransform(_params_2);
        for (int n = 0; n < _targCoords_2.length; n++) {
            mapPt.setLocation(_targCoords_2[n][0], _targCoords_2[n][1]);
            geoPt = trans.inverse(mapPt, geoPt);

            assertEquals(_srcCoords_2[n][1], geoPt.getLat(), _angleDelta);
            assertEquals(_srcCoords_2[n][0], geoPt.getLon(), _angleDelta);
        }
    }
}
