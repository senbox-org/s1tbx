/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.eo;

public final class Constants {
    public static final double secondsInDay = 86400.0;
    public static final double lightSpeed = 299792458.0; //  m / s
    public static final double halfLightSpeed = lightSpeed / 2.0;
    public static final double lightSpeedInMetersPerDay = Constants.lightSpeed * secondsInDay;

    public static final double semiMajorAxis = GeoUtils.WGS84.a; // in m, WGS84 semi-major axis of Earth
    public static final double semiMinorAxis = GeoUtils.WGS84.b; // in m, WGS84 semi-minor axis of Earth

    public static final double MeanEarthRadius = 6371008.7714; // in m (WGS84)

    public static final double oneMillion = 1000000.0;
    public static final double tenMillion = 10000000.0;
    public static final double oneBillion = 1000000000.0;
    public static final double oneBillionth = 1.0 / oneBillion;

    public static final double PI = 3.14159265358979323846264338327950288;
    public static final double TWO_PI = 2.0 * PI;
    public static final double HALF_PI = PI * 0.5;
    public static final double _PI = 3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348;
    public static final double _TWO_PI = 2.0 * _PI;

    public static final double DTOR = PI / 180.0;
    public static final double RTOD = 180.0 / PI;

    public static final double _DTOR = _PI / 180.0;
    public static final double _RTOD = 180.0 / _PI;

    public static final double EPS = 1e-15;

    public static final String USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM = "Use projected local incidence angle from DEM";
    public static final String USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM = "Use local incidence angle from DEM";
    public static final String USE_INCIDENCE_ANGLE_FROM_ELLIPSOID = "Use incidence angle from Ellipsoid";

    public static final double NO_DATA_VALUE = -99999.0;

    private Constants() {
    }
}