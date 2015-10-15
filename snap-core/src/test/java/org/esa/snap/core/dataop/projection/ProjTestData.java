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

package org.esa.snap.core.dataop.projection;

/**
 * @author Marco Peters
 * @since BEAM 4.8
 */
class ProjTestData {
    private final double lon;
    private final double lat;
    private final double mapX;
    private final double mapY;
    private final double lonInv;
    private final double latInv;

    // Used if result of inverse is same to input to forward transform
    ProjTestData(double lon, double lat, double mapX, double mapY) {
        this(lon, lat, mapX, mapY, lon, lat);
    }

    // Used if result of inverse is different to input to forward transform
    ProjTestData(double lon, double lat, double mapX, double mapY, double lonInv, double latInv) {
        this.lon = lon;
        this.lat = lat;
        this.mapX = mapX;
        this.mapY = mapY;
        this.lonInv = lonInv;
        this.latInv = latInv;
    }

    double getLon() {
        return lon;
    }

    double getLat() {
        return lat;
    }

    double getMapX() {
        return mapX;
    }

    double getMapY() {
        return mapY;
    }

    double getLonInv() {
        return lonInv;
    }

    double getLatInv() {
        return latInv;
    }
}
