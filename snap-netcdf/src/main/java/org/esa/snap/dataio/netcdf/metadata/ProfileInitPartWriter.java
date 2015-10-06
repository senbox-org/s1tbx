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
import org.esa.snap.dataio.netcdf.ProfileWriteContext;

import java.io.IOException;

/**
 * Implementations write the basic structure of a {@link Product Product} to a NetCDF-file. They are provided to the framework by implementations
 * of {@link org.esa.snap.dataio.netcdf.AbstractNetCdfWriterPlugIn AbstractNetCdfWriterPlugIn}.
 */
public interface ProfileInitPartWriter {
    /**
     * Writes the basic product body.
     *
     * @param ctx the context for writing the product body
     * @param product the product to write
     * @throws IOException if an IO-Error occurs
     */
    void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException;

}
