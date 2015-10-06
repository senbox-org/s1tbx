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
 * Implementations provide parts of a {@link Product Product}. They are provided to the framework by implementations
 * of {@link org.esa.snap.dataio.netcdf.AbstractNetCdfReaderPlugIn AbstractNetCdfReaderPlugIn}.
 * The methods of all {@link ProfilePartReader} belonging to a {@link org.esa.snap.dataio.netcdf.AbstractNetCdfReaderPlugIn AbstractNetCdfReaderPlugIn}
 * are called in the following sequence
 * <ol>
 * <li> {@link #preDecode(org.esa.snap.dataio.netcdf.ProfileReadContext , Product)}</li>
 * <li> {@link #decode(org.esa.snap.dataio.netcdf.ProfileReadContext , Product)}</li>
 * </ol>
 * If two parts of one {@link org.esa.snap.dataio.netcdf.AbstractNetCdfReaderPlugIn AbstractNetCdfReaderPlugIn}
 * implementation depend on each other, the twofold decoding helps to transport information from one part to the other.
 * One part can store information in the {@link ProfileReadContext context} and the other can retrieve it in the
 * successive decoding step.
 */
public interface ProfilePartReader {

    /**
     * The first step of the decoding.
     *
     * @param ctx the context for reading the product
     * @param p   the product currently read
     * @throws IOException if an IO-Error occurs
     */
    void preDecode(ProfileReadContext ctx, Product p) throws IOException;

    /**
     * The second step of the decoding.
     *
     * @param ctx the context for reading the product
     * @param p   the product currently read
     * @throws IOException if an IO-Error occurs
     */
    void decode(ProfileReadContext ctx, Product p) throws IOException;

}
