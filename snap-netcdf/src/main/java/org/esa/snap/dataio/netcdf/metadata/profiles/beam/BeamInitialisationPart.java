/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfInitialisationPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.util.Constants;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;

import java.io.IOException;

public class BeamInitialisationPart extends CfInitialisationPart {

    public static final String PRODUCT_TYPE = "product_type";

    @Override
    public Product readProductBody(ProfileReadContext ctx) throws ProductIOException {
        Dimension x = null;
        Dimension y = null;
        for (Dimension dimension : ctx.getNetcdfFile().getDimensions()) {
            final String name = dimension.getShortName();
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
                (String) ctx.getProperty(Constants.PRODUCT_FILENAME_PROPERTY),
                readProductType(ctx),
                x.getLength(),
                y.getLength()
        );
    }

    @Override
    public void writeProductBody(ProfileWriteContext ctx, Product p) throws IOException {
        super.writeProductBody(ctx, p);
        NFileWriteable writeable = ctx.getNetcdfFileWriteable();
        writeable.addGlobalAttribute(PRODUCT_TYPE, p.getProductType());
        writeable.addGlobalAttribute("metadata_profile", "beam");
        writeable.addGlobalAttribute("metadata_version", "0.5");
    }

    @Override
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
