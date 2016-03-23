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

package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.dataio.EncodeQualification;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.AbstractNetCdfWriterPlugIn;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NWritableFactory;
import org.esa.snap.dataio.netcdf.util.Constants;

import java.io.IOException;
import java.util.Locale;

public class CfNetCdfWriterPlugIn extends AbstractNetCdfWriterPlugIn {
    @Override
    public EncodeQualification getEncodeQualification(Product product) {
        if (product.isMultiSize()) {
            return new EncodeQualification(EncodeQualification.Preservation.UNABLE,
                                           "Cannot write multisize products. Consider resampling the product first.");
        }
        return new EncodeQualification(EncodeQualification.Preservation.PARTIAL);
    }

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
    public NFileWriteable createWritable(String outputPath) throws IOException {
        return NWritableFactory.create(outputPath, "netcdf3");
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
