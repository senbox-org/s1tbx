package org.esa.beam.framework.datamodel;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * The base class for {@link PlacemarkDescriptor} implementations.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public abstract class AbstractPlacemarkDescriptor implements PlacemarkDescriptor {

    /**
     * {@inheritDoc}
     * <p/>
     * The default implementation does nothing.
     *
     * @param featureType The feature type whose user data may or may not be altered.
     * @see org.opengis.feature.simple.SimpleFeatureType#getUserData()
     */
    @Override
    public void setUserData(SimpleFeatureType featureType) {
    }

    /**
     * {@inheritDoc}
     * <p/>
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
}
