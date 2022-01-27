/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.commons.product;

public class Missions {

    public static final String CAPELLA = "Capella";
    public static final String ICEYE = "Iceye";
    public static final String KOMPSAT2 = "Kompsat-2";
    public static final String KOMPSAT3 = "Kompsat-3";
    public static final String KOMPSAT5 = "Kompsat-5";
    public static final String LANDSAT8 = "Landsat-8";
    public static final String PLANETSCOPE = "PlanetScope";
    public static final String PLEIADES = "Pleiades";
    public static final String RADARSAT1 = "RS1";
    public static final String RADARSAT2 = "RS2";
    public static final String RCM = "RCM";
    public static final String SAOCOM = "SAOCOM";
    public static final String SENTINEL1 = "Sentinel-1";
    public static final String SENTINEL2 = "Sentinel-2";
    public static final String SENTINEL3 = "Sentinel-3";
    public static final String SKYSAT = "SkySat";
    public static final String SPOT = "SPOT";
    public static final String SUPERVIEW = "SuperView";
    public static final String TRIPLESAT = "TripleSat";
    public static final String WORLDVIEW1 = "WorldView-1";
    public static final String WORLDVIEW2 = "WorldView-2";
    public static final String WORLDVIEW3 = "WorldView-3";
    public static final String WORLDVIEW4 = "WorldView-4";

    public static String[] getList() {
        return new String[]{
                CAPELLA,
                ICEYE,
                KOMPSAT2,
                KOMPSAT3,
                KOMPSAT5,
                LANDSAT8,
                PLANETSCOPE,
                PLEIADES,
                RADARSAT1,
                RADARSAT2,
                RCM,
                SAOCOM,
                SENTINEL1,
                SENTINEL2,
                SENTINEL3,
                SKYSAT,
                SPOT,
                SUPERVIEW,
                TRIPLESAT,
                WORLDVIEW1,
                WORLDVIEW2,
                WORLDVIEW3,
                WORLDVIEW4
        };
    }
}
