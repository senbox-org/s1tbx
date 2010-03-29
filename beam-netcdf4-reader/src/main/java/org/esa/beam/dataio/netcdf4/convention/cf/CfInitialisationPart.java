package org.esa.beam.dataio.netcdf4.convention.cf;

import org.esa.beam.dataio.netcdf4.Nc4Constants;
import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.convention.InitialisationPart;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class CfInitialisationPart implements InitialisationPart {

    @Override
    public Product readProductBody(String productName, Nc4ReaderParameters rp) throws ProductIOException {
        return new Product(
                productName,
                getProductType(rp),
                rp.getRasterDigest().getRasterDim().getDimX().getLength(),
                rp.getRasterDigest().getRasterDim().getDimY().getLength()
        );
    }

    @Override
    public void writeProductBody(NetcdfFileWriteable writeable, Product p) throws IOException {
        writeable.addDimension("y", p.getSceneRasterHeight());
        writeable.addDimension("x", p.getSceneRasterWidth());
    }

    public static String getProductType(final Nc4ReaderParameters rv) {
        String productType = rv.getGlobalAttributes().getStringValue("Conventions");
        if (productType == null) {
            productType = Nc4Constants.FORMAT_NAME;
        }
        return productType;
    }
}
