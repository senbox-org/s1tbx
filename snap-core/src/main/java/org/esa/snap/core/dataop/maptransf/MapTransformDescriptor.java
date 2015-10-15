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

import org.esa.snap.core.param.Parameter;

/**
 * A descriptor for map transformation types.
 * 
 * @deprecated since BEAM 4.7, use geotools {@link org.geotools.referencing.operation.MathTransformProvider} instead.
 */
@Deprecated
public interface MapTransformDescriptor {

    /**
     * This method is called within the <code>{@link MapProjectionRegistry#registerDescriptor}</code>
     * method after an instance of this <code>MapTransformDescriptor</code> has been successfully registered. The method
     * can and should be used to register projections that are based on the type of <code>{@link MapTransform}</code>
     * described by this <code>MapTransformDescriptor</code>. Registering projection instances is done using the using
     * the <code>{@link MapProjectionRegistry#registerProjection}</code> method.
     * <p>
     * <p>
     * A typical implementation of this method would be:
     * <pre>
     * public void registerProjections() {
     *     MapProjectionRegistry.registerProjection(new MapProjection("my-projection-name-1", new
     * MyMapTransform(param_1)));
     *     MapProjectionRegistry.registerProjection(new MapProjection("my-projection-name-2", new
     * MyMapTransform(param_2)));
     *     MapProjectionRegistry.registerProjection(new MapProjection("my-projection-name-3", new
     * MyMapTransform(param_3)));
     *     ...
     * }
     * </pre>
     */
    void registerProjections();

    /**
     * Creates an instance of the map transform for the given parameter values. The parameter value array must exactly
     * match the size and semantics of the parameters returned by the {@link #getParameters()} method.
     * <p>Important: Implementors of this method shall ensure that an element-wise copy of the given parameter array is
     * created and set.
     *
     * @param parameterValues the parameter values. If null, a map transform with default parameter values is created
     *
     * @return a new instance of a map transform with the array of parameters being copied, never null
     */
    MapTransform createTransform(double[] parameterValues);

    /**
     * @return The unique type identifier for the map transformation, e.g. "Transverse_Mercator".
     */
    String getTypeID();

    /**
     * @return A descriptive name for this map transformation descriptor, e.g. "Transverse Mercator".
     */
    String getName();

    /**
     * @return The unit of the map, e.g. "degree" or "meter".
     */
    String getMapUnit();

    /**
     * Gets a copy of the default parameter values for this map transform. Changing elements in the returned array
     * will not affect this object's state.
     *
     * @return The default parameter values for this map transform.
     */
    double[] getParameterDefaultValues();

    /**
     * Gets the parameters for this map transform.
     * <p>Important: Changing elements in the returned array may change this object's state.
     *
     * @return The list of parameters required to create a new instance of the map transform.
     */
    Parameter[] getParameters();

    /**
     * Tests if a user interface is available.
     *
     * @return <code>true</code> if a user interface is available, in this case the {@link #getTransformUI} method never
     *         returns null.
     */
    boolean hasTransformUI();

    /**
     * Gets a user interface for editing the transformation properties of a map projection.
     *
     * @param transform the transformation which provides the default properties for the UI.
     *
     * @return the user interface or null if editing is not supported. The {@link #hasTransformUI} method shall return
     *         <code>false</code> in this case.
     */
    MapTransformUI getTransformUI(MapTransform transform);
}
