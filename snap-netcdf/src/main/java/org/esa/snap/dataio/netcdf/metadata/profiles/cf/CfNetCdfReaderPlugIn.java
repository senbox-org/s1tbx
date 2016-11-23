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

package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.dataio.netcdf.AbstractNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartReader;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.snap.dataio.netcdf.util.RasterDigest;
import ucar.nc2.NetcdfFile;

import java.util.Locale;

public class CfNetCdfReaderPlugIn extends AbstractNetCdfReaderPlugIn {

    @Override
    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        RasterDigest rasterDigest = RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
        if (rasterDigest != null && rasterDigest.getRasterVariables().length > 0) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"NetCDF-CF", "NetCDF4-CF"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".nc", ".nc.gz", ".nc.bz2", ".grb", ".grb.gz"};
    }

    @Override
    public String getDescription(Locale locale) {
        return "NetCDF following CF-Convention";
    }

    @Override
    public ProfileInitPartReader createInitialisationPartReader() {
        return new CfInitialisationPart();
    }

    @Override
    public ProfilePartReader createMetadataPartReader() {
        return new CfMetadataPart();
    }

    @Override
    public ProfilePartReader createBandPartReader() {
        return new CfBandPart();
    }

    @Override
    public ProfilePartReader createFlagCodingPartReader() {
        return new CfFlagCodingPart();
    }

    @Override
    public ProfilePartReader createGeoCodingPartReader() {
        return new CfGeocodingPart();
    }

    @Override
    public ProfilePartReader createTiePointGridPartReader() {
        return new CfTiePointGridPart();
    }

    @Override
    public ProfilePartReader createTimePartReader() {
        return new CfTimePart();
    }

    @Override
    public ProfilePartReader createDescriptionPartReader() {
        return new CfDescriptionPart();
    }
}
