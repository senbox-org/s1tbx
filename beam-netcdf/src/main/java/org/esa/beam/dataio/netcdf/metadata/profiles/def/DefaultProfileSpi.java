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

package org.esa.beam.dataio.netcdf.metadata.profiles.def;

import org.esa.beam.dataio.netcdf.metadata.AbstractProfileSpi;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPart;
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfDescriptionPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfEndTimePart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfStartTimePart;
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
public class DefaultProfileSpi extends AbstractProfileSpi {

    @Override
    public ProfilePart createMetadataPart() {
        return new DefaultMetadataPart();
//        return new CfMetadataPart();
    }

    @Override
    public ProfilePart createBandPart() {
        return new DefaultBandPart();
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
        return new DefaultFlagCodingPart();
    }

    @Override
    public ProfilePart createGeocodingPart() {
        return new DefaultGeocodingPart();
    }

    @Override
    public ProfilePart createImageInfoPart() {
        return new DefaultImageInfoPart();
    }

    @Override
    public ProfilePart createIndexCodingPart() {
        return new DefaultIndexCodingPart();
    }

    @Override
    public ProfileInitPart createInitialisationPart() {
        return new DefaultInitialisationPart();
    }

    @Override
    public ProfilePart createMaskOverlayPart() {
        return new DefaultMaskOverlayPart();
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
        return new BeamFileFilter("BEAM", Constants.FILE_EXTENSIONS, "A BEAM-NetCDF-product");
    }

    @Override
    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        Attribute attribute = netcdfFile.findGlobalAttribute("metadata_profile");
        if (attribute != null) {
            String value = attribute.getStringValue();
            if (value != null && value.equals("beam")) {
                return DecodeQualification.INTENDED;
            }
        }
        return DecodeQualification.UNABLE;
    }
}
