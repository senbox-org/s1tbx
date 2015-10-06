package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.DecodeQualification;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.Image;
import java.awt.Point;

/**
 * The base class for {@link PlacemarkDescriptor} implementations.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public abstract class AbstractPlacemarkDescriptor implements PlacemarkDescriptor {

    public final static String PROPERTY_NAME_PLACEMARK_DESCRIPTOR = "placemarkDescriptor";
    public final static String PROPERTY_NAME_DEFAULT_GEOMETRY = "defaultGeometry";

    /**
     * {@inheritDoc}
     * The default implementation returns
     * <pre>
     *     new Placemark(this, feature)
     * </pre>.
     *
     * @param feature The feature to be wrapped.
     */
    @Override
    public Placemark createPlacemark(SimpleFeature feature) {
        return new Placemark(this, feature);
    }

    // todo - add API doc describing what the method does (nf,ts - 2012-04-23)
    @Override
    public DecodeQualification getCompatibilityFor(SimpleFeatureType featureType) {
        // todo - this is not sufficient: in order to return INTENDED, the given featureType must be equal-to or derived-from baseFeatureType (nf - 2012-04-23)
        if (getBaseFeatureType().getTypeName().equals(featureType.getTypeName())) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    // todo - add API doc describing what the method does (nf,ts - 2012-04-23)
    @Override
    public void setUserDataOf(SimpleFeatureType compatibleFeatureType) {

        compatibleFeatureType.getUserData().put(PROPERTY_NAME_PLACEMARK_DESCRIPTOR, getClass().getName());

        final org.opengis.feature.type.GeometryDescriptor geometryDescriptor = compatibleFeatureType.getGeometryDescriptor();
        if (geometryDescriptor != null) {
            compatibleFeatureType.getUserData().put(PROPERTY_NAME_DEFAULT_GEOMETRY, geometryDescriptor.getLocalName());
        }
    }


    @Override
    public PlacemarkGroup getPlacemarkGroup(Product product) {
        final VectorDataNode[] nodes = product.getVectorDataGroup().toArray(new VectorDataNode[0]);
        for (VectorDataNode node : nodes) {
            if (node.getPlacemarkDescriptor() == this) {
                return node.getPlacemarkGroup();
            }
        }
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
    public Image getCursorImage() {
        return null;
    }

    @Override
    public Point getCursorHotSpot() {
        return new Point();
    }
}
