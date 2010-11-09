package org.esa.beam.dataio.netcdf.metadata.profiles.beam;

import org.esa.beam.dataio.netcdf.AbstractNetCdfReaderPlugIn;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartReader;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfDescriptionPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfTimePart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.DecodeQualification;
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
        return new String[]{"NetCDF-BEAM"};
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
