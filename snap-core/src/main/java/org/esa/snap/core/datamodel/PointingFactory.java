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

import org.esa.snap.core.dataio.ProductReader;

/**
 * A factory which creates instances of a {@link Pointing} for a given raster data node.
 * A <code>PointingFactory</code> is usually assigned to data {@link Product} by its {@link ProductReader ProductReader}
 */
public interface PointingFactory {


    /**
     * Retrieves the product types for which this instance can create {@link Pointing pointings}.
     *
     * @return the product types
     */
    String[] getSupportedProductTypes();

    /**
     * Creates a {@link Pointing} applicable to the given raster. It is ensured that this method
     * is only called for rasters which are contained in a {@link Product} and have a valid {@link GeoCoding}.
     *
     * @param raster the raster data node for which the {@link Pointing} is being created
     *
     * @return the pointing or null if it cannot be created
     */
    Pointing createPointing(RasterDataNode raster);
}
