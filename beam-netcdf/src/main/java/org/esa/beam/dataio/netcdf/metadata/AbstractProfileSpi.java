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

import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.dataio.netcdf.util.VariableMap;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
 * Abstract base class for factories which are able to create profiles for NetCDF4-IO. Subclasses allow to
 * dynamically add generic {@link ProfilePart}s to the model.
 */
public abstract class AbstractProfileSpi implements ProfileSpi {

    public abstract ProfileInitPart createInitialisationPart();

    public abstract ProfilePart createMetadataPart();

    public abstract ProfilePart createBandPart();

    public abstract ProfilePart createFlagCodingPart();

    public abstract ProfilePart createGeocodingPart();

    public abstract ProfilePart createImageInfoPart();

    public abstract ProfilePart createIndexCodingPart();

    public abstract ProfilePart createMaskPart();

    public abstract ProfilePart createStxPart();

    public abstract ProfilePart createTiePointGridPart();

    public abstract ProfilePart createTimePart();

    public abstract ProfilePart createDescriptionPart();

    @Override
    public ProfileReadContext createReadContext(NetcdfFile netcdfFile) throws IOException {
        final RasterDigest rasterDigest = RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
        final VariableMap variableMap = rasterDigest != null ? new VariableMap(
                rasterDigest.getRasterVariables()) : null;
        return new ProfileReadContextImpl(netcdfFile, rasterDigest, variableMap);
    }

    @Override
    public void configureProfile(NetcdfFile netcdfFile, Profile profile) throws IOException {
        profile.setInitialisationPart(createInitialisationPart());
        addProfilePart(profile, createMetadataPart());
        addProfilePart(profile, createBandPart());
        addProfilePart(profile, createTiePointGridPart());
        addProfilePart(profile, createFlagCodingPart());
        addProfilePart(profile, createGeocodingPart());
        addProfilePart(profile, createImageInfoPart());
        addProfilePart(profile, createIndexCodingPart());
        addProfilePart(profile, createMaskPart());
        addProfilePart(profile, createStxPart());
        addProfilePart(profile, createTimePart());
        addProfilePart(profile, createDescriptionPart());
    }

    protected void addProfilePart(Profile profile, ProfilePart profilePart) {
        if (profilePart != null) {
            profile.addProfilePart(profilePart);
        }
    }

}
