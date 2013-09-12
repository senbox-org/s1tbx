package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author Marco Peters
 */
class ExpectedGeoCoding {

    @JsonProperty(required = true)
    private ExpectedGeoCoordinate[] coordinates;
    @JsonProperty()
    private Float reverseAccuracy;
    @JsonProperty()
    private Class<? extends GeoCoding> geoCodingClass;


    ExpectedGeoCoding() {
        reverseAccuracy = 1.0e-2f;
    }

    ExpectedGeoCoding(Product product, Random random) {
        this();
        final ArrayList<Point2D> pointList = ExpectedPixel.createPointList(product, random);
        final GeoCoding geoCoding = product.getGeoCoding();
        geoCodingClass = geoCoding.getClass();
        coordinates = new ExpectedGeoCoordinate[pointList.size()];
        for (int i = 0; i < pointList.size(); i++) {
            Point2D point = pointList.get(i);
            final float x = (float) point.getX();
            final float y = (float) point.getY();
            final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
            coordinates[i] = new ExpectedGeoCoordinate(x, y, geoPos.getLat(), geoPos.getLon());
        }
    }

    public ExpectedGeoCoordinate[] getCoordinates() {
        return coordinates;
    }

    Float getReverseAccuracy() {
        return reverseAccuracy;
    }

    Class<? extends GeoCoding> getGeoCodingClass() {
        return geoCodingClass;
    }
}
