/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.pcidsk;

import org.apache.commons.math3.util.FastMath;
import org.esa.snap.engine_utilities.eo.Constants;

/*
 * Author: Sami Salkosuo, sami.salkosuo@fi.ibm.com
 *
 * (c) Copyright IBM Corp. 2007
 * GNU General Public License v2.0
 */

public class UTM2LatLon {
    private final static String southernHemisphere = "ACDEFGHJKLM";
    private final static double a = Constants.semiMajorAxis;
    private final static double e = 0.081819191;
    private final static double e1sq = 0.006739497;
    private final static double k0 = 0.9996;

    double _a3;

    private static String getHemisphere(final String latZone) {
        if (southernHemisphere.contains(latZone)) {
            return "S";
        }
        return "N";
    }

    public double[] convertUTMToLatLong(final String UTM) {
        final String[] utm = UTM.trim().split(" ");
        final int zone = Integer.parseInt(utm[0]);
        final String latZone = utm[1];
        final double easting = Double.parseDouble(utm[2]);
        double northing = Double.parseDouble(utm[3]);
        final String hemisphere = getHemisphere(latZone);

        if (hemisphere.equals("S")) {
            northing = Constants.oneMillion - northing;
        }
        double latitude = calcLatitude(northing, easting);

        double zoneCM = 3.0;
        if (zone > 0) {
            zoneCM = 6 * zone - 183.0;
        }

        double longitude = zoneCM - _a3;
        if (hemisphere.equals("S")) {
            latitude = -latitude;
        }

        final double[] latlon = {0.0, 0.0};
        latlon[0] = latitude;
        latlon[1] = longitude;
        return latlon;
    }

    private double calcLatitude(final double northing, final double easting) {
        final double arc = northing / k0;
        final double mu = arc
                / (a * (1 - FastMath.pow(e, 2) / 4.0 - 3 * FastMath.pow(e, 4) / 64.0 - 5 * FastMath.pow(e, 6) / 256.0));

        final double ei = (1 - FastMath.pow((1 - e * e), (1 / 2.0)))
                / (1 + FastMath.pow((1 - e * e), (1 / 2.0)));

        final double ca = 3 * ei / 2 - 27 * FastMath.pow(ei, 3) / 32.0;

        final double cb = 21 * FastMath.pow(ei, 2) / 16.0 - 55.0 * FastMath.pow(ei, 4) / 32.0;
        final double cc = 151 * FastMath.pow(ei, 3) / 96.0;
        final double cd = 1097 * FastMath.pow(ei, 4) / 512.0;
        final double phi1 = mu + ca * FastMath.sin(2 * mu) + cb * FastMath.sin(4 * mu) + cc * FastMath.sin(6 * mu) + cd
                * FastMath.sin(8 * mu);

        final double n0 = a / FastMath.pow((1 - FastMath.pow((e * FastMath.sin(phi1)), 2)), (1 / 2.0));

        final double r0 = a * (1 - e * e) / FastMath.pow((1 - FastMath.pow((e * FastMath.sin(phi1)), 2)), (3 / 2.0));
        final double fact1 = n0 * FastMath.tan(phi1) / r0;

        final double _a1 = 500000 - easting;
        final double dd0 = _a1 / (n0 * k0);
        final double fact2 = dd0 * dd0 / 2.0;

        final double t0 = FastMath.pow(FastMath.tan(phi1), 2);
        final double Q0 = e1sq * FastMath.pow(FastMath.cos(phi1), 2);
        final double fact3 = (5 + 3 * t0 + 10 * Q0 - 4 * Q0 * Q0 - 9 * e1sq) * FastMath.pow(dd0, 4) / 24.0;

        final double fact4 = (61 + 90 * t0 + 298 * Q0 + 45 * t0 * t0 - 252 * e1sq - 3 * Q0
                * Q0)
                * FastMath.pow(dd0, 6) / 720.0;

        final double lof1 = _a1 / (n0 * k0);
        final double lof2 = (1 + 2 * t0 + Q0) * FastMath.pow(dd0, 3) / 6.0;
        final double lof3 = (5 - 2 * Q0 + 28 * t0 - 3 * FastMath.pow(Q0, 2) + 8 * e1sq + 24 * FastMath.pow(t0, 2))
                * FastMath.pow(dd0, 5) / 120.0;
        final double _a2 = (lof1 - lof2 + lof3) / FastMath.cos(phi1);
        _a3 = _a2 * 180 / Math.PI;

        final double latitude = 180 * (phi1 - fact1 * (fact2 + fact3 + fact4)) / Math.PI;
        return latitude;
    }
}
