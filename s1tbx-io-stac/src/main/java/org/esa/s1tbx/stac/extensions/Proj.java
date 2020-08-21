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

public class Proj {

    public final static String proj = "proj";

    //REQUIRED. EPSG code of the datasource
    public final static String epsg = "proj:epsg";

    //WKT2 string representing the Coordinate Reference System (CRS) that the proj:geometry and proj:bbox fields represent
    public final static String wkt2 = "proj:wkt2";

    //PROJJSON object representing the Coordinate Reference System (CRS) that the proj:geometry and proj:bbox fields represent
    public final static String projjson = "proj:projjson";

    //Defines the footprint of this Item.
    public final static String geometry = "proj:geometry";

    //Bounding box of the Item in the asset CRS in 2 or 3 dimensions.
    public final static String bbox = "proj:bbox";

    //Centroid Object	Coordinates representing the centroid of the Item in the asset CRS
    public final static String centroid = "proj:centroid";

    //Number of pixels in Y and X directions for the default grid
    public final static String shape = "proj:shape";

    //The affine transformation coefficients for the default grid
    public final static String transform = "proj:transform";
}
