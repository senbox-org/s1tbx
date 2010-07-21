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

package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.metadata.AbstractProfileSpi;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPart;
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.profiles.def.DefaultImageInfoPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.def.DefaultMaskOverlayPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.def.DefaultStxPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.def.DefaultTiePointGridPart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * A profile used for reading/writing general NetCDF/CF files.
 *
 * @author Thomas Storm
 */
public class CfProfileSpi extends AbstractProfileSpi {

    @Override
    public ProfilePart createBandPart() {
        return new CfBandPart();
    }

    @Override
    public ProfilePart createDescriptionPart() {
        return new CfDescriptionPart();
    }

    @Override
    public ProfilePart createEndTimePart() {
        return new CfEndTimePart();
    }

    @Override
    public ProfilePart createFlagCodingPart() {
        return new CfFlagCodingPart();
    }

    @Override
    public ProfilePart createGeocodingPart() {
        return new CfGeocodingPart();
    }

    @Override
    public ProfilePart createImageInfoPart() {
        return new DefaultImageInfoPart();
    }

    @Override
    public ProfilePart createIndexCodingPart() {
        return new CfIndexCodingPart();
    }

    @Override
    public ProfileInitPart createInitialisationPart() {
        return new CfInitialisationPart();
    }

    @Override
    public ProfilePart createMaskOverlayPart() {
        return new DefaultMaskOverlayPart();
    }

    @Override
    public ProfilePart createMetadataPart() {
        return new CfMetadataPart();
    }

    @Override
    public ProfilePart createStartTimePart() {
        return new CfStartTimePart();
    }

    @Override
    public ProfilePart createStxPart() {
        return new DefaultStxPart();
    }

    @Override
    public ProfilePart createTiePointGridPart() {
        return new DefaultTiePointGridPart();
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter("netCDF/CF", Constants.FILE_EXTENSIONS, "netCDF/CF compliant products");
    }

    @Override
    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        Variable hdfEosVariable = netcdfFile.getRootGroup().findVariable("StructMetadata.0");
        if (hdfEosVariable != null) {
            // we dont't want to handle HDF EOS here
            return DecodeQualification.UNABLE;
        }
        RasterDigest rasterDigest = RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
        if (rasterDigest != null && rasterDigest.getRasterVariables().length > 0) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }
}
