package org.esa.beam.dataio.netcdf.metadata.profiles.beam;

import org.esa.beam.dataio.netcdf.AbstractNetCdfWriterPlugIn;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfDescriptionPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfTimePart;
import org.esa.beam.dataio.netcdf.util.Constants;

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
