package org.esa.beam.dataio.netcdf4.convention.cf;

import org.esa.beam.dataio.netcdf4.Nc4RasterDigest;
import org.esa.beam.dataio.netcdf4.convention.AbstractProfileSpi;
import org.esa.beam.dataio.netcdf4.convention.ProfileInitPart;
import org.esa.beam.dataio.netcdf4.convention.ProfilePart;
import org.esa.beam.dataio.netcdf4.convention.beam.BeamImageInfoPart;
import org.esa.beam.dataio.netcdf4.convention.beam.BeamMaskOverlayPart;
import org.esa.beam.dataio.netcdf4.convention.beam.BeamStxPart;
import org.esa.beam.dataio.netcdf4.convention.beam.BeamTiePointGridPart;
import org.esa.beam.framework.dataio.DecodeQualification;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 *  A profile used for reading/writing general NetCDF/CF files.
 * @author Thomas Storm
 */
public class CfProfileSpi extends AbstractProfileSpi {

    @Override
    public ProfilePart getBandPart() {
        return new CfBandPart();
    }

    @Override
    public ProfilePart getDescriptionPart() {
        return new CfDescriptionPart();
    }

    @Override
    public ProfilePart getEndTimePart() {
        return new CfEndTimePart();
    }

    @Override
    public ProfilePart getFlagCodingPart() {
        return new CfFlagCodingPart();
    }

    @Override
    public ProfilePart getGeocodingPart() {
        return new CfGeocodingPart();
    }

    @Override
    public ProfilePart getImageInfoPart() {
        return new BeamImageInfoPart();
    }

    @Override
    public ProfilePart getIndexCodingPart() {
        return new CfIndexCodingPart();
    }

    @Override
    public ProfileInitPart getInitialisationPart() {
        return new CfInitialisationPart();
    }

    @Override
    public ProfilePart getMaskOverlayPart() {
        return new BeamMaskOverlayPart();
    }

    @Override
    public ProfilePart getMetadataPart() {
        return new CfMetadataPart();
    }

    @Override
    public ProfilePart getStartTimePart() {
        return new CfStartTimePart();
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
        Variable hdfEosVariable = netcdfFile.getRootGroup().findVariable("StructMetadata.0");
        if (hdfEosVariable != null) {
            // we dont't want to handle HDF EOS here
            return DecodeQualification.UNABLE;
        }
        Nc4RasterDigest rasterDigest = Nc4RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
        if (rasterDigest != null && rasterDigest.getRasterVariables().length > 0) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }
}
