package org.esa.beam.framework.datamodel;

import java.awt.Image;
import java.awt.Point;

/**
* todo - add api doc
*
* @author Norman Fomferra
*/
class DefaultPlacemarkDescriptor implements PlacemarkDescriptor {
    @Override
    public String getShowLayerCommandId() {
        return null;
    }

    @Override
    public String getRoleName() {
        return "geometry";
    }

    @Override
    public String getRoleLabel() {
        return "geometry";
    }

    @Override
    public Image getCursorImage() {
        return null;
    }

    @Override
    public PlacemarkGroup getPlacemarkGroup(Product product) {
        return null;
    }

    @Override
    public PlacemarkSymbol createDefaultSymbol() {
        return null;
    }

    @Override
    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        return null;
    }

    @Override
    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        return null;
    }

    @Override
    public Point getCursorHotSpot() {
        return null;
    }
}
