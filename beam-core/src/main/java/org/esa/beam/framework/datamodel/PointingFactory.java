/*
 * $Id: PointingFactory.java,v 1.2 2006/09/18 06:34:20 marcop Exp $
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
package org.esa.beam.framework.datamodel;

/**
 * A factory which creates instances of a {@link Pointing} for a given raster data node.
 * A <code>PointingFactory</code> is usually assigned to data {@link Product} by its {@link org.esa.beam.framework.dataio.ProductReader ProductReader}
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
