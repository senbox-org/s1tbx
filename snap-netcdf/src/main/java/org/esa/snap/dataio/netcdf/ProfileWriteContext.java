/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.dataio.netcdf.nc.NFileWriteable;

/**
 * A context for writing metadata from the BEAM product model into NetCDF.
 * While writing a product this context can be used to store properties to
 * share them between multiple {@link org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter ProfilePartWriter}.
 */
public interface ProfileWriteContext extends PropertyStore {

    /**
     * Returns th instance of {@link org.esa.snap.dataio.netcdf.nc.NFileWriteable} which is used during writing.
     *
     * @return the {@link org.esa.snap.dataio.netcdf.nc.NFileWriteable}
     */
    public NFileWriteable getNetcdfFileWriteable();

}
