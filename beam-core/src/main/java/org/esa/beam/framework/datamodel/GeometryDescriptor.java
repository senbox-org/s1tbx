package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;

/**
 * A {@link PlacemarkDescriptor} that is used to create placemarks that
 * are actually plain geometries.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class GeometryDescriptor extends AbstractPlacemarkDescriptor {

    private static final SimpleFeatureType DEFAULT_FEATURE_TYPE = PlainFeatureFactory.createDefaultFeatureType();

    @Override
    public DecodeQualification getQualification(SimpleFeatureType featureType) {
        if (featureType.getTypeName().equals("org.esa.beam.Geometry")) {
            return DecodeQualification.INTENDED;
        } else if (featureType.getGeometryDescriptor() != null) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public SimpleFeatureType getBaseFeatureType() {
        return DEFAULT_FEATURE_TYPE;
    }

    @Override
    public void setUserData(SimpleFeatureType featureType) {
        super.setUserData(featureType);
        featureType.getUserData().put("defaultGeometry", featureType.getGeometryDescriptor().getLocalName());
    }

    @Override
    @Deprecated
    public String getShowLayerCommandId() {
        return null;
    }

    @Override
    @Deprecated
    public String getRoleName() {
        return "geometry";
    }

    @Override
    @Deprecated
    public String getRoleLabel() {
        return "geometry";
    }

    @Override
    @Deprecated
    public Image getCursorImage() {
        return null;
    }

    @Override
    @Deprecated
    public PlacemarkGroup getPlacemarkGroup(Product product) {
        return null;
    }

    @Override
    @Deprecated
    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        return null;
    }

    @Override
    @Deprecated
    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        return null;
    }

    @Override
    @Deprecated
    public Point getCursorHotSpot() {
        return null;
    }
}
