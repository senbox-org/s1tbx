package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;

import java.util.Random;

/**
 * @author Marco Peters
 */
class ExpectedGeoCoding {
    @JsonProperty(required = true)
    private ExpectedGeoCoordinate[] coordinates;
    @JsonProperty()
    private Float reverseAccuracy;


    ExpectedGeoCoding() {
        reverseAccuracy = 1.0e-2f;
    }

    ExpectedGeoCoding(Product product, Random random) {
        this();
        final float x = random.nextFloat() * product.getSceneRasterWidth();
        final float y = random.nextFloat() * product.getSceneRasterHeight();
        final GeoCoding geoCoding = product.getGeoCoding();
        final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
        final ExpectedGeoCoordinate expectedCoordinate = new ExpectedGeoCoordinate(x, y, geoPos.getLat(), geoPos.getLon());

        coordinates = new ExpectedGeoCoordinate[] {expectedCoordinate};
        reverseAccuracy = 1.0e-2f;
    }

    public ExpectedGeoCoordinate[] getCoordinates() {
        return coordinates;
    }

    Float getReverseAccuracy() {
        return reverseAccuracy;
    }
}
