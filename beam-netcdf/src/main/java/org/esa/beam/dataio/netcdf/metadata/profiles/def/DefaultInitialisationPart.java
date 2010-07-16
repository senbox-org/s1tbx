package org.esa.beam.dataio.netcdf.metadata.profiles.def;

import org.esa.beam.dataio.netcdf.metadata.ProfileInitPart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfInitialisationPart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class DefaultInitialisationPart implements ProfileInitPart {

    public static final String PRODUCT_TYPE = "product_type";

    @Override
    public Product readProductBody(ProfileReadContext ctx) throws ProductIOException {
        Dimension x = null;
        Dimension y = null;
        for (Dimension dimension : ctx.getNetcdfFile().getDimensions()) {
            final String name = dimension.getName();
            if ("x".equalsIgnoreCase(name) || "lon".equalsIgnoreCase(name)) {
                x = dimension;
            } else if ("y".equalsIgnoreCase(name) || "lat".equalsIgnoreCase(name)) {
                y = dimension;
            }
        }
        if (x == null || y == null) {
            throw new ProductIOException("Illegal Dimensions: Dimensions named x and y expected.");
        }
        return new Product(
                (String) ctx.getProperty(Constants.PRODUCT_NAME_PROPERTY_NAME),
                getProductType(ctx),
                x.getLength(),
                y.getLength()
        );
    }

    @Override
    public void writeProductBody(NetcdfFileWriteable writeable, Product p) throws IOException {
        new CfInitialisationPart().writeProductBody(writeable, p);
        writeable.addAttribute(null, new Attribute(PRODUCT_TYPE, p.getProductType()));
        writeable.addAttribute(null, new Attribute("metadata_profile", "beam"));
        writeable.addAttribute(null, new Attribute("metadata_version", "0.5"));
        writeable.addAttribute(null, new Attribute("Conventions", "CF-1.4"));
    }

    public static String getProductType(ProfileReadContext ctx) {
        final Attribute productTypeAtt = ctx.getNetcdfFile().getRootGroup().findAttribute(PRODUCT_TYPE);
        if (productTypeAtt != null) {
            final String pt = productTypeAtt.getStringValue();
            if (pt != null && pt.trim().length() > 0) {
                return pt.trim();
            }
        }
        return CfInitialisationPart.getProductType(ctx);
    }
}
