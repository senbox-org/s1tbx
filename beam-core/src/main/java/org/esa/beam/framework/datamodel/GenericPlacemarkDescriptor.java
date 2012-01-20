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
public class GenericPlacemarkDescriptor extends AbstractPlacemarkDescriptor {

    private final SimpleFeatureType featureType;

    public GenericPlacemarkDescriptor(SimpleFeatureType featureType) {
        this.featureType = featureType;
    }

    @Override
    public boolean isCompatibleWith(SimpleFeatureType ft) {
        return featureType.equals(ft);
    }

    @Override
    public SimpleFeatureType getBaseFeatureType() {
        return featureType;
    }

    @Override
    @Deprecated
    public String getRoleName() {
        return "default";
    }

    @Override
    @Deprecated
    public String getRoleLabel() {
        return "default";
    }

    @Override
    @Deprecated
    public PlacemarkGroup getPlacemarkGroup(Product product) {
        return null;
    }

    @Override
    @Deprecated
    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        return pixelPos;
    }

    @Override
    @Deprecated
    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        return geoPos;
    }

    @Override
    @Deprecated
    public String getShowLayerCommandId() {
        return null;
    }

    @Override
    @Deprecated
    public Image getCursorImage() {
        return null;
    }

    @Override
    @Deprecated
    public Point getCursorHotSpot() {
        return null;
    }
}
