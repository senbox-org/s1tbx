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

package org.esa.snap.dataio.netcdf.metadata;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileReadContext;

import java.io.IOException;

/**
 * Implementations provide the initialisation part of a {@link Product Product}. They are provided to the framework by implementations
 * of {@link org.esa.snap.dataio.netcdf.AbstractNetCdfReaderPlugIn AbstractNetCdfReaderPlugIn}.
 */
public interface ProfileInitPartReader {
    /**
     * Reads the basic product body.
     *
     * @param ctx Provides the context for reading the product body.
     * @return the product
     * @throws IOException if an IO-Error occurs
     */
    Product readProductBody(ProfileReadContext ctx) throws IOException;

}
