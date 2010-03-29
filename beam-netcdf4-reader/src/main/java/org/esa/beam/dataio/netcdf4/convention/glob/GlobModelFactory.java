package org.esa.beam.dataio.netcdf4.convention.glob;

import org.esa.beam.dataio.netcdf4.convention.AbstractModelFactory;
import org.esa.beam.dataio.netcdf4.convention.InitialisationPart;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.dataio.netcdf4.convention.beam.BeamImageInfoPart;
import org.esa.beam.dataio.netcdf4.convention.beam.BeamMaskOverlayPart;
import org.esa.beam.dataio.netcdf4.convention.beam.BeamTiePointGridPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfDescriptionPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfEndTimePart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfFlagCodingPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfGeocodingPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfIndexCodingPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfInitialisationPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfMetadataPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfStartTimePart;

/**
 * User: Thomas Storm
 * Date: 29.03.2010
 * Time: 13:36:13
 */
public class GlobModelFactory extends AbstractModelFactory {

    @Override
    public ModelPart getBandPart() {
        return new GlobBandPart();
    }

    @Override
    public ModelPart getDescriptionPart() {
        return new CfDescriptionPart();
    }

    @Override
    public ModelPart getEndTimePart() {
        return new CfEndTimePart();
    }

    @Override
    public ModelPart getFlagCodingPart() {
        return new CfFlagCodingPart();
    }

    @Override
    public ModelPart getGeocodingPart() {
        return new CfGeocodingPart();
    }

    @Override
    public ModelPart getImageInfoPart() {
        return new BeamImageInfoPart();
    }

    @Override
    public ModelPart getIndexCodingPart() {
        return new CfIndexCodingPart();
    }

    @Override
    public InitialisationPart getInitialisationPart() {
        return new CfInitialisationPart();
    }

    @Override
    public ModelPart getMaskOverlayPart() {
        return new BeamMaskOverlayPart();
    }

    @Override
    public ModelPart getMetadataPart() {
        return new CfMetadataPart();
    }

    @Override
    public ModelPart getStartTimePart() {
        return new CfStartTimePart();
    }

    @Override
    public ModelPart getStxPart() {
        return null;
    }

    @Override
    public ModelPart getTiePointGridPart() {
        return new BeamTiePointGridPart();
    }
}
