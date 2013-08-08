package org.esa.beam.dataio;

/**
 * @author Marco Peters
 */
public class ExpectedGeoCoding {
    private ExpectedGeoCoordinates[] coordinates;

    public ExpectedGeoCoding() {
        coordinates = new ExpectedGeoCoordinates[0];
    }

    public ExpectedGeoCoordinates[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(ExpectedGeoCoordinates[] coordinates) {
        this.coordinates = coordinates;
    }
}
