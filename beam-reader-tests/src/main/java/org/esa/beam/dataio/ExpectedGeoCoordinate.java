package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;

/**
 * @author Marco Peters
 */
class ExpectedGeoCoordinate {
    @JsonProperty(required = true)
    private Double x;
    @JsonProperty(required = true)
    private Double y;
    @JsonProperty(required = true)
    private Double lat;
    @JsonProperty(required = true)
    private Double lon;

    ExpectedGeoCoordinate() {
    }

    public ExpectedGeoCoordinate(double x, double y, double lat, double lon) {
        this();
        this.x = x;
        this.y = y;
        this.lat = lat;
        this.lon = lon;
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    PixelPos getPixelPos() {
        return new PixelPos(getX(), getY());
    }

    double getLat() {
        return lat;
    }

    double getLon() {
        return lon;
    }

    GeoPos getGeoPos() {
        return new GeoPos(getLat(), getLon());
    }
}
