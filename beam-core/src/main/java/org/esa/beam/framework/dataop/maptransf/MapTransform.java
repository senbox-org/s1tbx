/*
 * $Id: MapTransform.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.dataop.maptransf;

import org.esa.beam.framework.datamodel.GeoPos;

import java.awt.geom.Point2D;

/**
 * Provides a parameterized, mathematical algorithm for a map transformation.
 */
public interface MapTransform {

    /**
     * Gets the descriptor for this map transform.
     *
     * @return the descriptor, should never be <code>null</code>
     */
    MapTransformDescriptor getDescriptor();

    /**
     * Gets the array of parameter values. The order in which the parameters are returned must exactly match the
     * order in which the corresponding {@link org.esa.beam.framework.param.Parameter} array is returned by the
     * <code>{@link MapTransformDescriptor#getParameters()}</code> method.
     * <p/>
     * <p>Important: Implementors of this method shall ensure that an element-wise copy of the given parameter array is
     * created and returned.</p>
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
