package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.metadata.ProfileInitPart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class CfInitialisationPart implements ProfileInitPart {

    @Override
    public Product readProductBody(ProfileReadContext ctx) throws ProductIOException {
        return new Product(
                (String) ctx.getProperty(Constants.PRODUCT_NAME_PROPERTY_NAME),
                getProductType(ctx),
                ctx.getRasterDigest().getRasterDim().getDimX().getLength(),
                ctx.getRasterDigest().getRasterDim().getDimY().getLength()
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

    public static String getProductType(final ProfileReadContext ctx) {
        Attribute productType = ctx.getNetcdfFile().findGlobalAttribute("Conventions");
        if (productType != null) {
            return productType.getStringValue();
        } else {
            return Constants.FORMAT_NAME;
        }
    }
}
