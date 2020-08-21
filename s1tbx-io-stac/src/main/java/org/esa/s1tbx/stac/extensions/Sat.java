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

public class Sat {

    public final static String sat = "sat";

    //The state of the orbit. Either ascending or descending for polar orbiting satellites, or geostationary for geosynchronous satellites
    public final static String orbit_state = "sat:orbit_state";

    //The relative orbit number at the time of acquisition.
    public final static String relative_orbit = "sat:relative_orbit";

    public final static String ascending = "ascending";
    public final static String descending = "descending";
    public final static String geosynchronous = "geosynchronous";
}
