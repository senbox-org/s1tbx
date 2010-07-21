package org.esa.beam.framework.dataop.projection;

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