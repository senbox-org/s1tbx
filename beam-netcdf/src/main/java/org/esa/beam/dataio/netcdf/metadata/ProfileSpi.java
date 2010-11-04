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

package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
 * The service provider interface (SPI) for a specific NetCDF metadata profile
 * capable of representing the BEAM product data model.
 * <p/>
 * This interface is intended to be implemented by clients.
 * But its should not be implemented directly. Instead, clients are asked to
 * derive from {@link AbstractProfileSpi}.
 *
 * @deprecated no replacement
 */
@Deprecated
public interface ProfileSpi {

    /**
     * Configures a metadata profile for the given NetcDF file.
     *
     * @param netcdfFile The NetcDF file.
     * @param profile    The profile.
     *
     * @throws IOException If an I/O error occurs.
     */
    void configureProfile(NetcdfFile netcdfFile, Profile profile) throws IOException;

    /**
     * Detects whether a profile can be generated for the given NetCDF file.
     *
     * @param netcdfFile The NetcDF file.
     *
     * @return A decode qualification.
     */
    DecodeQualification getDecodeQualification(NetcdfFile netcdfFile);

    ProfileReadContext createReadContext(NetcdfFile netcdfFile) throws IOException;

    /**
     * Gets an instance of {@link org.esa.beam.util.io.BeamFileFilter} for use in a {@link javax.swing.JFileChooser JFileChooser}.
     *
     * @return a file filter
     */
    BeamFileFilter getProductFileFilter();
}
