package org.esa.beam.framework.datamodel;

import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.Image;
import java.awt.Point;

/**
 * Used as a fallback for the case that
 * we can't find any suitable {@link PlacemarkDescriptor} for a given SimpleFeatureType.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class DefaultPlacemarkDescriptor extends AbstractPlacemarkDescriptor {

    public DefaultPlacemarkDescriptor() {
    }

    @Override
    public SimpleFeatureType getDefaultFeatureType() {
        return null;
    }

    @Override
    public boolean isCompatibleWith(SimpleFeatureType ft) {
        return true;
    }

    @Override
    public String getRoleName() {
        return "default";
    }

    @Override
    public String getRoleLabel() {
        return "default";
    }

    @Override
    public PlacemarkGroup getPlacemarkGroup(Product product) {
        return null;
    }

    @Override
    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        return pixelPos;
    }

    @Override
    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        return geoPos;
    }

    @Override
    public String getShowLayerCommandId() {
        return null;
    }

    @Override
    public PlacemarkSymbol createDefaultSymbol() {
        return null;
    }

    @Override
    public Image getCursorImage() {
        return null;
    }

    @Override
    public Point getCursorHotSpot() {
        return null;
    }
}
