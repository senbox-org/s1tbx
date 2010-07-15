package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.FileInfo;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPart;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class CfInitialisationPart implements ProfileInitPart {

    @Override
    public Product readProductBody(String productName, FileInfo rp) throws ProductIOException {
        return new Product(
                productName,
                getProductType(rp),
                rp.getRasterDigest().getRasterDim().getDimX().getLength(),
                rp.getRasterDigest().getRasterDim().getDimY().getLength()
        );
    }

    @Override
    public void writeProductBody(NetcdfFileWriteable writeable, Product product) throws IOException {
        if (CfGeocodingPart.isGeographicLatLon(product.getGeoCoding())) {
            addDimensions(writeable, product, "lat", "lon");
        } else {
            addDimensions(writeable, product, "y", "x");
        }
    }

    private void addDimensions(NetcdfFileWriteable writeable, Product p, String dimY, String dimX) {
        writeable.addDimension(dimY, p.getSceneRasterHeight());
        writeable.addDimension(dimX, p.getSceneRasterWidth());
    }

    public static String getProductType(final FileInfo rv) {
        String productType = rv.getGlobalAttributes().getStringValue("Conventions");
        if (productType == null) {
            productType = Constants.FORMAT_NAME;
        }
        return productType;
    }
}
