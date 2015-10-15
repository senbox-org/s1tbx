package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.DecodeQualification;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * A {@link PlacemarkDescriptor} that is used to create placemarks that
 * are actually plain geometries.
 *
 * @author Norman Fomferra
 * @author Thomas Storm
 * @since BEAM 4.10
 */
public class GeometryDescriptor extends AbstractPlacemarkDescriptor {

    private static final SimpleFeatureType DEFAULT_FEATURE_TYPE = PlainFeatureFactory.createDefaultFeatureType();

    @Override
    public DecodeQualification getCompatibilityFor(SimpleFeatureType featureType) {
        final DecodeQualification qualification = super.getCompatibilityFor(featureType);
        if (qualification ==  DecodeQualification.INTENDED) {
            return DecodeQualification.INTENDED;
        } else if (featureType.getGeometryDescriptor() != null) {
            return DecodeQualification.SUITABLE;
        }
        return qualification;
    }

    @Override
    public SimpleFeatureType getBaseFeatureType() {
        return DEFAULT_FEATURE_TYPE;
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
}
