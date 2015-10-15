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
package org.esa.snap.core.dataop.maptransf;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.param.Parameter;

import java.awt.geom.Point2D;

/**
 * The descriptor for a map transformation which represents the identity transformation.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * 
 * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.PlateCarree.Provider} instead.
 */
@Deprecated
public class IdentityTransformDescriptor implements MapTransformDescriptor {

    public static final String TYPE_ID = "Identity";
    public static final String NAME = "Geographic Lat/Lon";
    public static final String MAP_UNIT = "degree";
    public static final Parameter[] PARAMETERS = new Parameter[] {};
    public static final double[] PARAMETER_DEFAULT_VALUES = new double[] {};

    public IdentityTransformDescriptor() {
    }

    public void registerProjections() {
        MapProjectionRegistry.registerProjection(new MapProjection(getName(), createTransform(null), true));
    }

    /**
     * Gets a descriptive name for this map transformation descriptor.
     */
    public String getName() {
        return NAME;
    }

    public String getTypeID() {
        return TYPE_ID;
    }

    public String getMapUnit() {
        return MAP_UNIT;
    }

    public Parameter[] getParameters() {
        return new Parameter[] {};
    }

    /**
     * Gets the default parameter values for this map transform.
     */
    public double[] getParameterDefaultValues() {
        return new double[] {};
    }

    /**
     * Tests if a user interface is available. Returns <code>false</code> because a user interface is not available for
     * this descriptor.
     *
     * @return always <code>false</code>
     */
    public boolean hasTransformUI() {
        return false;
    }

    /**
     * Gets a user interface for editing the transformation properties of a map projection. Returns <code>null</code>
     * because a user interface is not available for this descriptor.
     *
     * @param transform ignored
     *
     * @return always <code>null</code>
     */
    public MapTransformUI getTransformUI(MapTransform transform) {
        return null;
    }

    public MapTransform createTransform(double[] parameterValues) {
        return new IMT();
    }

    private class IMT implements MapTransform {

        private IMT() {
        }

        public MapTransformDescriptor getDescriptor() {
            return IdentityTransformDescriptor.this;
        }

        public double[] getParameterValues() {
            return new double[] {};
        }

        /**
         * Forward project geographical co-ordinates into map co-ordinates.
         */
        public Point2D forward(GeoPos geoPoint, Point2D mapPoint) {
            if (mapPoint != null) {
                mapPoint.setLocation(geoPoint.lon, geoPoint.lat);
            } else {
                mapPoint = new Point2D.Double(geoPoint.lon, geoPoint.lat);
            }
            return mapPoint;
        }

        /**
         * Inverse project map co-ordinates into geographical co-ordinates.
         */
        public GeoPos inverse(Point2D mapPoint, GeoPos geoPoint) {
            if (geoPoint != null) {
                geoPoint.setLocation(mapPoint.getY(), mapPoint.getX());
            } else {
                geoPoint = new GeoPos(mapPoint.getY(), mapPoint.getX());
            }
            return geoPoint;
        }

        public MapTransform createDeepClone() {
            return new IMT();
        }
    }
}
