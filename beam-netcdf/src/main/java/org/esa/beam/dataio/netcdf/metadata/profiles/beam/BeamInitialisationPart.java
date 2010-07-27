/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.netcdf.metadata.profiles.beam;

import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfInitialisationPart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class BeamInitialisationPart extends CfInitialisationPart {

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
            throw new ProductIOException("Illegal Dimensions: Dimensions named (x,lon) and (y,lat) expected.");
        }
        return new Product(
                (String) ctx.getProperty(Constants.PRODUCT_NAME_PROPERTY_NAME),
                readProductType(ctx),
                x.getLength(),
                y.getLength()
        );
    }

    @Override
    public void writeProductBody(NetcdfFileWriteable writeable, Product p) throws IOException {
        super.writeProductBody(writeable, p);
        writeable.addAttribute(null, new Attribute(PRODUCT_TYPE, p.getProductType()));
        writeable.addAttribute(null, new Attribute("metadata_profile", "beam"));
        writeable.addAttribute(null, new Attribute("metadata_version", "0.5"));
        writeable.addAttribute(null, new Attribute("Conventions", "CF-1.4"));
    }

    public String readProductType(ProfileReadContext ctx) {
        final Attribute productTypeAtt = ctx.getNetcdfFile().findGlobalAttribute(PRODUCT_TYPE);
        if (productTypeAtt != null) {
            final String pt = productTypeAtt.getStringValue();
            if (pt != null && pt.trim().length() > 0) {
                return pt.trim();
            }
        }
        return super.readProductType(ctx);
    }
}
