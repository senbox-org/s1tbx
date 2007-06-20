/*
 * $Id: MapTransformFactory.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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

import org.esa.beam.util.Guardian;

/**
 * A factory for map transformation instances.
 */
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
