package org.esa.beam.framework.datamodel;

import org.opengis.feature.simple.SimpleFeature;

/**
 * The base class for {@link PlacemarkDescriptor} implementations.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public abstract class AbstractPlacemarkDescriptor implements PlacemarkDescriptor {

    @Override
    public Placemark createPlacemark(SimpleFeature feature) {
        return new Placemark(this, feature);
    }
}
