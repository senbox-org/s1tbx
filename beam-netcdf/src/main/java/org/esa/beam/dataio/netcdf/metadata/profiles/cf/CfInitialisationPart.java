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

package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartIO;
import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;

public class CfInitialisationPart extends ProfileInitPartIO {

    @Override
    public Product readProductBody(ProfileReadContext ctx) throws ProductIOException {
        return new Product(
                (String) ctx.getProperty(Constants.PRODUCT_FILENAME_PROPERTY),
                readProductType(ctx),
                ctx.getRasterDigest().getRasterDim().getDimX().getLength(),
                ctx.getRasterDigest().getRasterDim().getDimY().getLength()
        );
    }

    @Override
    public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
        NetcdfFileWriteable writeable = ctx.getNetcdfFileWriteable();
        if (CfGeocodingPart.isGeographicCRS(product.getGeoCoding())) {
            writeDimensions(writeable, product, "lat", "lon");
        } else {
            writeDimensions(writeable, product, "y", "x");
        }
    }

    private void writeDimensions(NetcdfFileWriteable writeable, Product p, String dimY, String dimX) {
        writeable.addDimension(dimY, p.getSceneRasterHeight());
        writeable.addDimension(dimX, p.getSceneRasterWidth());
    }

    public String readProductType(final ProfileReadContext ctx) {
        Attribute productType = ctx.getNetcdfFile().findGlobalAttribute("Conventions");
        if (productType != null) {
            return productType.getStringValue();
        } else {
            return Constants.FORMAT_NAME;
        }
    }
}
