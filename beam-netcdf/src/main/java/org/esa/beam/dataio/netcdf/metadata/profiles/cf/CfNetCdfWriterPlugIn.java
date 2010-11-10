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

import org.esa.beam.dataio.netcdf.AbstractNetCdfWriterPlugIn;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.beam.dataio.netcdf.util.Constants;

import java.util.Locale;

public class CfNetCdfWriterPlugIn extends AbstractNetCdfWriterPlugIn {

    @Override
    public String[] getFormatNames() {
        return new String[]{"NetCDF-CF"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{Constants.FILE_EXTENSION_NC};
    }

    @Override
    public String getDescription(Locale locale) {
        return "NetCDF following CF-Convention";
    }

    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new CfInitialisationPart();
    }

    @Override
    public ProfilePartWriter createMetadataPartWriter() {
        return new CfMetadataPart();
    }

    @Override
    public ProfilePartWriter createBandPartWriter() {
        return new CfBandPart();
    }

    @Override
    public ProfilePartWriter createFlagCodingPartWriter() {
        return new CfFlagCodingPart();
    }

    @Override
    public ProfilePartWriter createGeoCodingPartWriter() {
        return new CfGeocodingPart();
    }

    @Override
    public ProfilePartWriter createTiePointGridPartWriter() {
        return new CfTiePointGridPart();
    }

    @Override
    public ProfilePartWriter createTimePartWriter() {
        return new CfTimePart();
    }

    @Override
    public ProfilePartWriter createDescriptionPartWriter() {
        return new CfDescriptionPart();
    }

}
