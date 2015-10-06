/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataio.DecodeQualification;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.Image;
import java.awt.Point;

/**
 * Placemark descriptors are used to describe and create {@link Placemark}s.
 * <p>
 * New placemark descriptors can be added by using the Service Provider Interface
 * {@code META-INF/services/PlacemarkDescriptor}.
 * <p>
 * Since this interface is likely to change, clients should not directly implement it.
 * Instead they should derive their implementation from {@link AbstractPlacemarkDescriptor}.
 *
 * @author Norman Fomferra
 * @author Thomas Storm
 * @version 2.0
 * @since BEAM 2.0 (full revision since BEAM 4.10)
 */
public interface PlacemarkDescriptor {

    /**
     * Creates a new placemark by wrapping the given feature.
     *
     * @param feature The feature to be wrapped.
     * @return The new placemark.
     */
    Placemark createPlacemark(SimpleFeature feature);

    /**
     * Asks the descriptor to set any application specific information in the feature type's user data.
     * When called, the framework has already classified the given feature type as being {@link DecodeQualification#INTENDED} or
     * {@link DecodeQualification#SUITABLE} by consulting the method {@link #getCompatibilityFor}.
     * <p>
     * Implementors of this method may consider setting specific user data that allows {@link #getCompatibilityFor}
     * to return {@link DecodeQualification#INTENDED} after e.g. data has been deserialized from a persistent data store.
     *
     * @param compatibleFeatureType A compatible feature type whose user data may or may not be altered.
     */
    void setUserDataOf(SimpleFeatureType compatibleFeatureType);

    /**
     * Gets the compatibility for the given {@code featureType} with respect to the placemarks created by this descriptor.
     * <p>
     * The method shall only return {@link DecodeQualification#INTENDED}, if the
     * descriptor was designed for interpreting the given feature type, e.g. by inspecting the {@code featureType}'s
     * user data which may be set by
     * {@link #setUserDataOf(org.opengis.feature.simple.SimpleFeatureType)}.
     * <p>
     * The method may return {@link DecodeQualification#SUITABLE}, if the
     * {@link #createPlacemark(org.opengis.feature.simple.SimpleFeature)}
     * method can successfully create a new placemark from a feature having the compatible {@code featureType}.
     *
     * @param featureType The feature type to be tested.
     * @return {@code true}, if the {@code featureType} is compatible.
     */
    DecodeQualification getCompatibilityFor(SimpleFeatureType featureType);

    /**
     * Gets the feature type that provides the minimum set of attributes
     * required for this placemark descriptor.
     *
     * @return The base feature type.
     */
    SimpleFeatureType getBaseFeatureType();

    // todo - remove deprecated methods (nf while revising Placemark API)

    @Deprecated
    String getRoleName();

    @Deprecated
    String getRoleLabel();

    @Deprecated
    PlacemarkGroup getPlacemarkGroup(Product product);

    @Deprecated
    PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos);

    @Deprecated
    GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos);

    // GUI-related stuff

    @Deprecated
    String getShowLayerCommandId();

    @Deprecated
    Image getCursorImage();

    @Deprecated
    Point getCursorHotSpot();
}
