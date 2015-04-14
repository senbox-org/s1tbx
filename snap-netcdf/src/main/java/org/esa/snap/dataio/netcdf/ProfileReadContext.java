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

package org.esa.snap.dataio.netcdf;

import org.esa.snap.dataio.netcdf.util.RasterDigest;
import ucar.nc2.NetcdfFile;

/**
 * A context for reading metadata from netCDF into the BEAM product model.
 * While reading a product this context can be used to store properties to
 * share them between multiple {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartReader ProfilePartReader}.
 */
public interface ProfileReadContext extends PropertyStore {

    /**
     * Gets the {@link NetcdfFile} to be read.
     *
     * @return the {@link NetcdfFile}
     */
    public NetcdfFile getNetcdfFile();

    /**
     * Sets the {@link RasterDigest}.
     *
     * @param rasterDigest the {@link RasterDigest}
     *
     * @see AbstractNetCdfReaderPlugIn#initReadContext(org.esa.snap.dataio.netcdf.ProfileReadContext)
     */
    public void setRasterDigest(RasterDigest rasterDigest);

    /**
     * Gets the {@link RasterDigest}.
     *
     * @return the {@link RasterDigest}
     */
    public RasterDigest getRasterDigest();


}
