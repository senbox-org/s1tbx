package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.Nc4DataTypeWorkarounds;
import org.esa.beam.dataio.netcdf4.Nc4RasterDigest;
import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.Nc4ReaderUtils;
import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfBandPart;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.jdom.Element;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;


public class HdfEosBandPart implements ModelPart {

    @Override
    public void read(Product p, Nc4ReaderParameters rp) throws IOException {
        final Variable[] variables = rp.getRasterDigest().getRasterVariables();
        for (Variable variable : variables) {
            final int rasterDataType = Nc4ReaderUtils.getRasterDataType(variable.getDataType(), variable.isUnsigned());
            final Band band = p.addBand(variable.getShortName(), rasterDataType);
            CfBandPart.applyAttributes(band, variable);
        }
    }

    @Override
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw) throws IOException {
        throw  new IllegalStateException();
    }
}
