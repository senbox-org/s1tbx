/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.vividsolutions.jts.geom.Point;
import org.esa.snap.core.dataio.DecodeQualification;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

/**
 * Placemark descriptor implementation for handling track data.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class TrackPointDescriptor extends PointPlacemarkDescriptor {

    public static TrackPointDescriptor getInstance() {
        return (TrackPointDescriptor) PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(TrackPointDescriptor.class.getName());
    }

    public TrackPointDescriptor() {
        super("org.esa.snap.TrackPoint");
    }

    @Override
    public DecodeQualification getCompatibilityFor(SimpleFeatureType featureType) {
        final GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        if (geometryDescriptor != null) {
            if (geometryDescriptor.getType().getBinding().equals(Point.class)) {
                final Object trackPoints = featureType.getUserData().get("trackPoints");
                if (trackPoints != null && Boolean.parseBoolean(trackPoints.toString())) {
                    return DecodeQualification.INTENDED;
                } else {
                    return DecodeQualification.SUITABLE;
                }
            }
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public void setUserDataOf(SimpleFeatureType compatibleFeatureType) {
        super.setUserDataOf(compatibleFeatureType);
        compatibleFeatureType.getUserData().put("trackPoints", "true");
    }

    @Override
    public String getRoleName() {
        return "track_point";
    }

    @Override
    public String getRoleLabel() {
        return "track point";
    }
}
