package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;

/**
 * @author Marco Peters
 */
class ExpectedGeoCoordinate {
    @JsonProperty(required = true)
    private Float x;
    @JsonProperty(required = true)
    private Float y;
    @JsonProperty(required = true)
    private Float lat;
    @JsonProperty(required = true)
    private Float lon;

    ExpectedGeoCoordinate() {
    }

    public ExpectedGeoCoordinate(float x, float y, float lat, float lon) {
        this();
        this.x = x;
        this.y = y;
        this.lat = lat;
        this.lon = lon;
    }

    float getX() {
        return x;
    }

    float getY() {
        return y;
    }

    PixelPos getPixelPos() {
        return new PixelPos(getX(), getY());
    }

    float getLat() {
        return lat;
    }

    float getLon() {
        return lon;
    }

    GeoPos getGeoPos() {
        return new GeoPos(getLat(), getLon());
    }
}
