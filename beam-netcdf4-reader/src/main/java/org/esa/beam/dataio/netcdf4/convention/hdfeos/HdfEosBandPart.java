package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.Nc4ReaderUtils;
import org.esa.beam.dataio.netcdf4.convention.Profile;
import org.esa.beam.dataio.netcdf4.convention.ProfilePart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfBandPart;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;


public class HdfEosBandPart extends ProfilePart {

    @Override
    public void read(Profile profile, Product p) throws IOException {
        final Variable[] variables = profile.getFileInfo().getRasterDigest().getRasterVariables();
        for (Variable variable : variables) {
            final int rasterDataType = Nc4ReaderUtils.getRasterDataType(variable.getDataType(), variable.isUnsigned());
            final Band band = p.addBand(variable.getShortName(), rasterDataType);
            CfBandPart.applyAttributes(band, variable);
        }
    }

    @Override
    public void define(Profile ctx, Product p, NetcdfFileWriteable ncFile) throws IOException {
        throw new IllegalStateException();
    }
}
