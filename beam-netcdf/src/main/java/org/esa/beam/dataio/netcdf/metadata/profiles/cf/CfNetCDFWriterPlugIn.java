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
