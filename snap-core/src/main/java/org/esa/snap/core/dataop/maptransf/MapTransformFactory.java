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

import org.esa.snap.core.util.Guardian;

/**
 * A factory for map transformation instances.
 * 
 * @deprecated since BEAM 4.7, use geotools instead.
 */
@Deprecated
public class MapTransformFactory {

    /**
     * Creates a new map transformation for the specified type ID, e.g. "Transverse_Mercator", and the array of
     * parameter values.
     *
     * @param typeID          the map transform type ID, e.g. "Transverse_Mercator", must not be null
     * @param parameterValues an array of parameter values
     *
     * @return a new map transformation instance of the specified type, or <code>null</code> if the given type is not
     *         registered
     */
    public static MapTransform createTransform(String typeID, double[] parameterValues) {
        Guardian.assertNotNullOrEmpty("typeID", typeID);
        MapTransformDescriptor transformDescriptor = MapProjectionRegistry.getDescriptor(typeID);
        if (transformDescriptor != null) {
            return transformDescriptor.createTransform(parameterValues);
        }
        return null;
    }
}
