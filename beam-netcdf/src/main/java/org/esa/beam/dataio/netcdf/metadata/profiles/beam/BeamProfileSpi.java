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

package org.esa.beam.dataio.netcdf.metadata.profiles.beam;

import org.esa.beam.dataio.netcdf.metadata.AbstractProfileSpi;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPart;
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfDescriptionPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfTimePart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

/**
 * The default profile used for BEAM NetCDF/CF files.
 *
 * @author Thomas Storm
 */
public class BeamProfileSpi extends AbstractProfileSpi {

    private static final String[] FILE_EXTENSIONS = new String[]{
            Constants.FILE_EXTENSION_NC, Constants.FILE_EXTENSION_NC_GZ
    };

    @Override
    public ProfilePart createMetadataPart() {
        return new BeamMetadataPart();
    }

    @Override
    public ProfilePart createBandPart() {
        return new BeamBandPart();
    }

    @Override
    public ProfilePart createDescriptionPart() {
        return new CfDescriptionPart();
    }

    @Override
    public ProfilePart createFlagCodingPart() {
        return new BeamFlagCodingPart();
    }

    @Override
    public ProfilePart createGeocodingPart() {
        return new BeamGeocodingPart();
    }

    @Override
    public ProfilePart createImageInfoPart() {
        return new BeamImageInfoPart();
    }

    @Override
    public ProfilePart createIndexCodingPart() {
        return new BeamIndexCodingPart();
    }

    @Override
    public ProfileInitPart createInitialisationPart() {
        return new BeamInitialisationPart();
    }

    @Override
    public ProfilePart createMaskPart() {
        return new BeamMaskPart();
    }

    @Override
    public ProfilePart createTimePart() {
        return new CfTimePart();
    }

    @Override
    public ProfilePart createStxPart() {
        return new BeamStxPart();
    }

    @Override
    public ProfilePart createTiePointGridPart() {
        return new BeamTiePointGridPart();
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter("BEAM", FILE_EXTENSIONS, "BEAM netCDF products");
    }

    @Override
    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        Attribute attribute = netcdfFile.getRootGroup().findAttribute("metadata_profile");
        if (attribute != null) {
            String value = attribute.getStringValue();
            if (value != null && value.equals("beam")) {
                return DecodeQualification.INTENDED;
            }
        }
        return DecodeQualification.UNABLE;
    }
}
