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

public class MapTransformUtilsTest extends TestCase {

    private double[] _exc1 = new double[]{1.1, 1.0, 0.95, 0.9, 0.85, 0.8};
    private double[] _exc2 = new double[]{0.006, 0.012, 0.003, 0.0001, 0.024, 0.03};
    private double[] _args = new double[]{0.71, -0.34, 0.56, -0.78, 1.23, -1.44};
    private double[] _expInvLength = new double[]{0.71330055126015,
                                                  -0.34389100235832,
                                                  0.56143567897512,
                                                  -0.78005700057606,
                                                  1.2430448502071,
                                                  -1.4536210890449};
    private double[][] _expLengthParams = new double[][]{
        {0.62664688110352, 0.72664688110352, 0.53943125406901, 0.47692000325521, 0.45038232421875},
        {0.67291259765625,
         0.67291259765625,
         0.44860839843750,
         0.35888671875000,
         0.30761718750000},
        {0.69474984169006,
         0.64474984169006,
         0.40608322779338,
         0.30794470723470,
         0.25055612182617},
        {0.71578506469727,
         0.61578506469727,
         0.36552337646484,
         0.26204370117188,
         0.20182763671875},
        {0.73606255531311,
         0.58606255531311,
         0.32695837020874,
         0.22092607116699,
         0.16057809448242},
        {0.75562500000000,
         0.55562500000000,
         0.29041666666667,
         0.18433333333333,
         0.12600000000000},
    };

    public MapTransformUtilsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(MapTransformUtilsTest.class);
    }

    public void testGetLengthParams() {
        double[] dRet = null;

        for (int n = 0; n < _exc1.length; n++) {
            dRet = MapTransformUtils.getLengthParams(_exc1[n]);
            assertEquals(5, dRet.length);

            for (int k = 0; k < dRet.length; k++) {
                assertEquals(_expLengthParams[n][k], dRet[k], 1e-8);
            }
        }
    }

    public void testInverseMeridionalLength() {
        double[] en = null;
        double retval = 0.0;

        for (int n = 0; n < _exc2.length; n++) {
            en = MapTransformUtils.getLengthParams(_exc2[n]);
            retval = MapTransformUtils.invMeridLength(_args[n], _exc2[n], en);
            assertEquals(_expInvLength[n], retval, 1e-8);
        }
    }

    public void test_msfn() {
        double[] sinPhi = {0.34202014332567,
                           0.86602540378444,
                           0.17364817766693,
                           0.50000000000000,
                           0.50000000000000
        };

        double[] cosPhi = {0.93969262078591,
                           0.50000000000000,
                           0.98480775301221,
                           0.86602540378444,
                           0.86602540378444
        };

        double[] es = {0.0066943799901413,
                       0.0066943799901413,
                       0.0066943799901413,
                       0.0066943799901413,
                       0.0066943177782667
        };

        double[] result = {0.94006077070724,
                           0.50125994266413,
                           0.98490716483566,
                           0.86675100257220,
                           0.86675099582062
        };

        double out;

        for (int n = 0; n < sinPhi.length; n++) {
            out = MapTransformUtils.msfn(sinPhi[n], cosPhi[n], es[n]);
            assertEquals(result[n], out, 1e-6);
        }
    }

    // tsfn
    //  sinphi              phi                 e                   result
    //  0.34202014332567    0.34906585039887    0.081819190842621   0.70181299875731
    //  0.86602540378444    1.0471975511966     0.081819190842621   0.26950976331260
    //  0.17364817766693    0.17453292519943    0.081819190842621   0.84007568959970
    //  0.64278760968654    0.69813170079773    0.081819190842621   0.46832039453376
    //  0.17364817766693    0.17453292519943    0.081818810662749   0.84007568052316
    //  0.64278760968654    0.69813170079773    0.081818810662749   0.46832037577147

    public void test_tsfn() {
        double[] sinPhi = {0.34202014332567,
                           0.86602540378444,
                           0.17364817766693,
                           0.64278760968654,
                           0.17364817766693,
                           0.64278760968654
        };

        double[] phi = {0.34906585039887,
                        1.0471975511966,
                        0.17453292519943,
                        0.69813170079773,
                        0.17453292519943,
                        0.69813170079773
        };

        double[] e = {0.081819190842621,
                      0.081819190842621,
                      0.081819190842621,
                      0.081819190842621,
                      0.081818810662749,
                      0.081818810662749
        };

        double[] result = {0.70181299875731,
                           0.26950976331260,
                           0.84007568959970,
                           0.46832039453376,
                           0.84007568052316,
                           0.46832037577147
        };

        double out;
        for (int n = 0; n < sinPhi.length; n++) {
            out = MapTransformUtils.tsfn(phi[n], sinPhi[n], e[n]);
            assertEquals(result[n], out, 1e-6);
        }
    }

    // phi2
    //  ts                      e                   result
    //  9.5905648772470e-006    0.081819190842621   1.5707772739278
    //  1.8541564827703e-005    0.081819190842621   1.5707594916372
    //  0.00014589008548276     0.081819190842621   1.5705064977368
    //  0.00039416643426756     0.081819190842621   1.5700132654843

    public void testPhi2() {
        double[] ts = {9.5905648772470e-006,
                       1.8541564827703e-005,
                       0.00014589008548276,
                       0.00039416643426756
        };

        double e = 0.081819190842621;

        double[] result = {1.5707772739278,
                           1.5707594916372,
                           1.5705064977368,
                           1.5700132654843
        };

        double out;
        for (int n = 0; n < ts.length; n++) {
            out = MapTransformUtils.phi2(ts[n], e);
            assertEquals(result[n], out, 1e-6);
        }
    }
}

