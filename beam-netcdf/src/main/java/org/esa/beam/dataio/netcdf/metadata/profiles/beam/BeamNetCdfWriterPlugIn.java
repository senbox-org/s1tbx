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

package org.esa.beam.dataio.netcdf.metadata.profiles.beam;

import org.esa.beam.dataio.netcdf.AbstractNetCdfWriterPlugIn;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfDescriptionPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfTimePart;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NWritableFactory;
import org.esa.beam.dataio.netcdf.util.Constants;

import java.io.IOException;
import java.util.Locale;

public class BeamNetCdfWriterPlugIn extends AbstractNetCdfWriterPlugIn {

    @Override
    public String[] getFormatNames() {
        return new String[]{"NetCDF-BEAM"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{Constants.FILE_EXTENSION_NC};
    }

    @Override
    public String getDescription(Locale locale) {
        return "BEAM NetCDF products";
    }

    @Override
    public NFileWriteable createWritable(String outputPath) throws IOException {
        return NWritableFactory.create(outputPath, "netcdf3");
    }

     @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new BeamInitialisationPart();
    }

    @Override
    public ProfilePartWriter createMetadataPartWriter() {
        return new BeamMetadataPart();
    }

    @Override
    public ProfilePartWriter createBandPartWriter() {
        return new BeamBandPart();
    }

    @Override
    public ProfilePartWriter createDescriptionPartWriter() {
        return new CfDescriptionPart();
    }

    @Override
    public ProfilePartWriter createFlagCodingPartWriter() {
        return new BeamFlagCodingPart();
    }

    @Override
    public ProfilePartWriter createGeoCodingPartWriter() {
        return new BeamGeocodingPart();
    }

    @Override
    public ProfilePartWriter createImageInfoPartWriter() {
        return new BeamImageInfoPart();
    }

    @Override
    public ProfilePartWriter createIndexCodingPartWriter() {
        return new BeamIndexCodingPart();
    }

    @Override
    public ProfilePartWriter createMaskPartWriter() {
        return new BeamMaskPart();
    }

    @Override
    public ProfilePartWriter createTimePartWriter() {
        return new CfTimePart();
    }

    @Override
    public ProfilePartWriter createStxPartWriter() {
        return new BeamStxPart();
    }

    @Override
    public ProfilePartWriter createTiePointGridPartWriter() {
        return new BeamTiePointGridPart();
    }
}
