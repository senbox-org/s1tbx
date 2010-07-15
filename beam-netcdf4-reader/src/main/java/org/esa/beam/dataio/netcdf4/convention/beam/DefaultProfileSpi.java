package org.esa.beam.dataio.netcdf4.convention.beam;

import org.esa.beam.dataio.netcdf4.convention.AbstractProfileSpi;
import org.esa.beam.dataio.netcdf4.convention.ProfileInitPart;
import org.esa.beam.dataio.netcdf4.convention.ProfilePart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfMetadataPart;
import org.esa.beam.framework.dataio.DecodeQualification;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

/**
 * The default profile used for BEAM NetCDF/CF files.
 * @author Thomas Storm
 */
public class DefaultProfileSpi extends AbstractProfileSpi {

    public DefaultProfileSpi() {
        super();
    }

    @Override
    public ProfilePart getMetadataPart() {
        return new CfMetadataPart();
    }

    @Override
    public ProfilePart getBandPart() {
        return new BeamBandPart();
    }

    @Override
    public ProfilePart getDescriptionPart() {
        return null;
    }

    @Override
    public ProfilePart getEndTimePart() {
        return null;
    }

    @Override
    public ProfilePart getFlagCodingPart() {
        return new BeamFlagCodingPart();
    }

    @Override
    public ProfilePart getGeocodingPart() {
        return new BeamGeocodingPart();
    }

    @Override
    public ProfilePart getImageInfoPart() {
        return new BeamImageInfoPart();
    }

    @Override
    public ProfilePart getIndexCodingPart() {
        return new BeamIndexCodingPart();
    }

    @Override
    public ProfileInitPart getInitialisationPart() {
        return new BeamInitialisationPart();
    }

    @Override
    public ProfilePart getMaskOverlayPart() {
        return new BeamMaskOverlayPart();
    }

    @Override
    public ProfilePart getStartTimePart() {
        return null;
    }

    @Override
    public ProfilePart getStxPart() {
        return new BeamStxPart();
    }

    @Override
    public ProfilePart getTiePointGridPart() {
        return new BeamTiePointGridPart();
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
