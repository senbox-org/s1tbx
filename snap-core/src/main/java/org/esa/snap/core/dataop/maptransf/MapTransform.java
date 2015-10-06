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
 * Provides a parameterized, mathematical algorithm for a map transformation.
 * 
 * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.projection.MapProjection} instead.
 */
@Deprecated
public interface MapTransform {

    /**
     * Gets the descriptor for this map transform.
     *
     * @return the descriptor, should never be <code>null</code>
     */
    MapTransformDescriptor getDescriptor();

    /**
     * Gets the array of parameter values. The order in which the parameters are returned must exactly match the
     * order in which the corresponding {@link Parameter} array is returned by the
     * <code>{@link MapTransformDescriptor#getParameters()}</code> method.
     * <p>Important: Implementors of this method shall ensure that an element-wise copy of the given parameter array is
     * created and returned.
     *
     * @return the array of parameter values.
     */
    double[] getParameterValues();

    /**
     * Forward project geographical co-ordinates into map co-ordinates.
     */
    Point2D forward(GeoPos geoPoint, Point2D mapPoint);

    /**
     * Inverse project map co-ordinates into geographical co-ordinates.
     */
    GeoPos inverse(Point2D mapPoint, GeoPos geoPoint);

    /**
     * Creates a deep clone of this <code>MapTransform</code>.
     *
     * @return a <code>MapTransform</code> clone
     */
    MapTransform createDeepClone();
}
