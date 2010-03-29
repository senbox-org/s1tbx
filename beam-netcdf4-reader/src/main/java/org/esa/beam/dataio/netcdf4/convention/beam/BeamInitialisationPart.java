package org.esa.beam.dataio.netcdf4.convention.beam;

import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.convention.InitialisationPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfInitialisationPart;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class BeamInitialisationPart implements InitialisationPart {

    public static final String PRODUCT_TYPE = "product_type";

    @Override
    public Product readProductBody(String productName, Nc4ReaderParameters rp) throws ProductIOException {
        Dimension x = null;
        Dimension y = null;
        for (Dimension dimension : rp.getNetcdfFile().getDimensions()) {
            final String name = dimension.getName();
            if ("x".equalsIgnoreCase(name)) {
                x = dimension;
            } else if ("y".equalsIgnoreCase(name)) {
                y = dimension;
            }
        }
        if (x == null || y == null) {
            throw new ProductIOException("Illegal Dimensions: Dimensions named x and y expected.");
        }
        return new Product(
                productName,
                getProductType(rp),
                x.getLength(),
                y.getLength()
        );
    }

    @Override
    public void writeProductBody(NetcdfFileWriteable writeable, Product p) throws IOException {
        writeable.addDimension(null, new Dimension("y", p.getSceneRasterHeight()));
        writeable.addDimension(null, new Dimension("x", p.getSceneRasterWidth()));
        writeable.addAttribute(null, new Attribute(PRODUCT_TYPE, p.getProductType()));
        writeable.addAttribute(null, new Attribute("metadata_profile", "beam"));
        writeable.addAttribute(null, new Attribute("metadata_version", "0.5"));
        writeable.addAttribute(null, new Attribute("Conventions", "CF-1.4"));
    }

    public static String getProductType(Nc4ReaderParameters rp) {
        final Attribute productTypeAtt = rp.getNetcdfFile().getRootGroup().findAttribute(PRODUCT_TYPE);
        if (productTypeAtt != null) {
            final String pt = productTypeAtt.getStringValue();
            if (pt != null && pt.trim().length() > 0) {
                return pt.trim();
            }
        }
        return CfInitialisationPart.getProductType(rp);
    }
}