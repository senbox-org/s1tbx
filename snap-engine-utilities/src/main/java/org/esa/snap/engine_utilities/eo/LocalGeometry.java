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

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;

/**
 * Created with IntelliJ IDEA.
 * User: lveci
 * Date: 03/01/13
 * Time: 10:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocalGeometry {

    public double leftPointLat;
    public double leftPointLon;
    public double rightPointLat;
    public double rightPointLon;
    public double upPointLat;
    public double upPointLon;
    public double downPointLat;
    public double downPointLon;
    public PosVector sensorPos;
    public PosVector centrePoint;

    public LocalGeometry(final int x, final int y, final TileGeoreferencing tileGeoRef,
                         final PosVector earthPoint, final PosVector sensorPos) {
        final GeoPos geo = new GeoPos();

        tileGeoRef.getGeoPos(x - 1, y, geo);
        this.leftPointLat = geo.lat;
        this.leftPointLon = geo.lon;

        tileGeoRef.getGeoPos(x + 1, y, geo);
        this.rightPointLat = geo.lat;
        this.rightPointLon = geo.lon;

        tileGeoRef.getGeoPos(x, y - 1, geo);
        this.upPointLat = geo.lat;
        this.upPointLon = geo.lon;

        tileGeoRef.getGeoPos(x, y + 1, geo);
        this.downPointLat = geo.lat;
        this.downPointLon = geo.lon;
        this.centrePoint = earthPoint;
        this.sensorPos = sensorPos;
    }

    public LocalGeometry(final double lat, final double lon, final double delLat, final double delLon,
                         final PosVector earthPoint, final PosVector sensorPos) {

        this.leftPointLat = lat;
        this.leftPointLon = lon - delLon;

        this.rightPointLat = lat;
        this.rightPointLon = lon + delLon;

        this.upPointLat = lat - delLat;
        this.upPointLon = lon;

        this.downPointLat = lat + delLat;
        this.downPointLon = lon;
        this.centrePoint = earthPoint;
        this.sensorPos = sensorPos;
    }

}
