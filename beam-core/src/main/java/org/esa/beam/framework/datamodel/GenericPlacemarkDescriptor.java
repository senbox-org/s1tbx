package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.opengis.feature.simple.SimpleFeatureType;

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
    public DecodeQualification getCompatibilityFor(SimpleFeatureType ft) {
        if (featureType.equals(ft)) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
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
}
