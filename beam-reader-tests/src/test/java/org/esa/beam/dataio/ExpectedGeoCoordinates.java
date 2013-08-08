package org.esa.beam.dataio;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;

/**
 * @author Marco Peters
 */
public class ExpectedGeoCoordinates {

    private Float x;
    private Float y;
    private Float lat;
    private Float lon;


    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    PixelPos getPixelPos() {
        return new PixelPos(getX(), getY());
    }

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    GeoPos getGeoPos() {
        return new GeoPos(getLat(), getLon());
    }
}
