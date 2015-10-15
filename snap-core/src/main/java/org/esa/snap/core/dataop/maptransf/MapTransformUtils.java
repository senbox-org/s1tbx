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

import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.math.MathUtils;

//@todo 1 se/** - add (more) class documentation
/**
 * 
 * @deprecated since BEAM 4.7, use geotools instead.
 */
@Deprecated
public class MapTransformUtils {

    private static double _c00 = 1.0;
    private static double _c02 = 0.25;
    private static double _c04 = 0.046875;
    private static double _c06 = 0.01953125;
    private static double _c08 = 0.01068115234375;
    private static double _c22 = 0.75;
    private static double _c44 = 0.46875;
    private static double _c46 = 0.01302083333333333333;
    private static double _c48 = 0.00712076822916666666;
    private static double _c66 = 0.36458333333333333333;
    private static double _c68 = 0.00569661458333333333;
    private static double _c88 = 0.3076171875;

    private static int _max_iter = 15;
    private static double _eps_10 = 1e-10;
    private static double _eps_11 = 1e-11;

    /**
     * Calculates meridinal distance for ellipsoid.
     *
     * @param phi
     * @param sphi
     * @param cphi
     * @param en
     */
    public static double meridLength(double phi, double sphi, double cphi, double[] en) {
        double a = cphi * sphi;
        double sphis = sphi * sphi;

        return (en[0] * phi - a * (en[1] + sphis * (en[2] + sphis * (en[3] + sphis * en[4]))));
    }

    /**
     * Calculates the inverse meridional distance for ellipsoid
     */
    public static double invMeridLength(double arg, double es, double[] en) {
        double k = 1.0 / (1.0 - es);
        double s;
        double t;
        double phi = arg;

        for (int n = 0; n < _max_iter; n++) {
            s = Math.sin(phi);
            t = 1.0 - es * s * s;
            t = (meridLength(phi, s, Math.cos(phi), en) - arg) * t * Math.sqrt(t) * k;
            phi -= t;
            if (Math.abs(t) < _eps_11) {
                return phi;
            }
        }
        Debug.trace("MapTransformUtils: invMeridLength() - exceeded maximum iterations");
        return phi;

    }

    /**
     * Retrieves a set of ellipse distance parameters for given excentricity
     */
    public static double[] getLengthParams(double es) {
        double[] dRet = new double[5];
        double t = es * es;

        dRet[0] = _c00 - es * (_c02 + es * (_c04 + es * (_c06 + es * _c08)));
        dRet[1] = es * (_c22 - es * (_c04 + es * (_c06 + es * _c08)));
        dRet[2] = t * (_c44 - es * (_c46 + es * _c48));
        t *= es;
        dRet[3] = t * (_c66 - es * _c68);
        dRet[4] = t * es * _c88;

        return dRet;
    }

    /**
     * This is an undocumented method copied one-to-one from the proj-4.4.7 source code. I have no idea what purpose it
     * serves ....
     *
     * @param sinphi
     * @param cosphi
     * @param es
     *
     * @return the calculated value
     */
    public static double msfn(double sinphi, double cosphi, double es) {
        return cosphi / Math.sqrt(1.0 - es * sinphi * sinphi);
    }

    /**
     * This is an undocumented method copied one-to-one from the proj-4.4.7 source code. I have no idea what purpose it
     * serves ....
     *
     * @param phi
     * @param sinphi
     * @param e
     *
     * @return the calculated value
     */
    public static double tsfn(double phi, double sinphi, double e) {
        double temp = sinphi * e;
        return Math.tan(0.5 * (MathUtils.HALFPI - phi)) / Math.pow((1.0 - temp) / (1.0 + temp), 0.5 * e);
    }

    /**
     * Also this one is from the proj-4.4.7 source code. The very sparse commments tell us: "determine latitude angle
     * phi-2"
     *
     * @param ts
     * @param e
     *
     * @return the calculated value
     */
    public static double phi2(double ts, double e) {
        double eccnth, phi, con, dphi;

        eccnth = 0.5 * e;
        phi = MathUtils.HALFPI - 2.0 * Math.atan(ts);

        for (int n = 0; n < _max_iter; n++) {
            con = e * Math.sin(phi);
            dphi = MathUtils.HALFPI - 2.0 * Math.atan(ts * Math.pow((1. - con) / (1. + con), eccnth)) - phi;
            phi += dphi;

            if (Math.abs(dphi) <= _eps_10) {
                return phi;
            }
        }

        Debug.trace("MapTransformUtils: phi2() - exceeded maximum iterations");
        return phi;
    }

    /**
     * This is an undocumented method copied one-to-one from the proj-4.4.7 source code. I have no idea what purpose it
     * serves ....
     *
     * @param phi
     * @param sinphi
     * @param e
     *
     * @return the calculated value
     */
    public static double ssfn(final double phi, double sinphi, final double e) {
        sinphi *= e;
        return (Math.tan(.5 * (MathUtils.HALFPI + phi)) *
                Math.pow((1. - sinphi) / (1. + sinphi), .5 * e));
    }
}
