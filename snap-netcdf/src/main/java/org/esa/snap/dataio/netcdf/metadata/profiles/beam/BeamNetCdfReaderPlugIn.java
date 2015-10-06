/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.dataio.netcdf.AbstractNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartReader;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfDescriptionPart;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfTimePart;
import org.esa.snap.dataio.netcdf.util.Constants;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.util.Locale;

public class BeamNetCdfReaderPlugIn extends AbstractNetCdfReaderPlugIn {

    @Override
    protected DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        Attribute attribute = netcdfFile.getRootGroup().findAttribute("metadata_profile");
        if (attribute != null) {
            String value = attribute.getStringValue();
            if (value != null && value.equals("beam")) {
                return DecodeQualification.INTENDED;
            }
        }
        return DecodeQualification.UNABLE;

    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"NetCDF-BEAM", "NetCDF4-BEAM"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{Constants.FILE_EXTENSION_NC, Constants.FILE_EXTENSION_NC_GZ};
    }

    @Override
    public String getDescription(Locale locale) {
        return "BEAM NetCDF products";
    }

    @Override
    public ProfileInitPartReader createInitialisationPartReader() {
        return new BeamInitialisationPart();
    }

    @Override
    public ProfilePartReader createMetadataPartReader() {
        return new BeamMetadataPart();
    }

    @Override
    public ProfilePartReader createBandPartReader() {
        return new BeamBandPart();
    }

    @Override
    public ProfilePartReader createDescriptionPartReader() {
        return new CfDescriptionPart();
    }

    @Override
    public ProfilePartReader createFlagCodingPartReader() {
        return new BeamFlagCodingPart();
    }

    @Override
    public ProfilePartReader createGeoCodingPartReader() {
        return new BeamGeocodingPart();
    }

    @Override
    public ProfilePartReader createImageInfoPartReader() {
        return new BeamImageInfoPart();
    }

    @Override
    public ProfilePartReader createIndexCodingPartReader() {
        return new BeamIndexCodingPart();
    }

    @Override
    public ProfilePartReader createMaskPartReader() {
        return new BeamMaskPart();
    }

    @Override
    public ProfilePartReader createTimePartReader() {
        return new CfTimePart();
    }

    @Override
    public ProfilePartReader createStxPartReader() {
        return new BeamStxPart();
    }

    @Override
    public ProfilePartReader createTiePointGridPartReader() {
        return new BeamTiePointGridPart();
    }
}
