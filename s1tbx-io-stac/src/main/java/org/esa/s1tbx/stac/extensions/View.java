/*
 * Copyright (C) 2020 Skywatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.stac.extensions;

public class View {

    public final static String view = "view";

    //The angle from the sensor between nadir (straight down) and the scene center. Measured in degrees (0-90).
    public final static String off_nadir = "view:off_nadir";

    //The incidence angle is the angle between the vertical (normal) to the intercepting surface and the line of sight
    //back to the satellite at the scene center. Measured in degrees (0-90).
    public final static String incidence_angle = "view:incidence_angle";

    //Viewing azimuth angle. The angle measured from the sub-satellite point (point on the ground below the platform)
    //between the scene center and true north. Measured clockwise from north in degrees (0-360).
    public final static String azimuth = "view:azimuth";

    //Sun azimuth angle. From the scene center point on the ground, this is the angle between truth north and the sun.
    //Measured clockwise in degrees (0-360).
    public final static String sun_azimuth = "view:sun_azimuth";

    //Sun elevation angle. The angle from the tangent of the scene center point to the sun. Measured from the horizon in degrees (0-90).
    public final static String sun_elevation = "view:sun_elevation";
}
