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

package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.util.Constants;
import ucar.nc2.Attribute;

import java.awt.Dimension;
import java.io.IOException;

public class CfInitialisationPart extends ProfileInitPartIO {

    @Override
    public Product readProductBody(ProfileReadContext ctx) throws ProductIOException {
        Product product = new Product(
                (String) ctx.getProperty(Constants.PRODUCT_FILENAME_PROPERTY),
                readProductType(ctx),
                ctx.getRasterDigest().getRasterDim().getDimensionX().getLength(),
                ctx.getRasterDigest().getRasterDim().getDimensionY().getLength()
        );
        Attribute tileSize = ctx.getNetcdfFile().findGlobalAttribute("TileSize");
        if (tileSize != null) {
            String stringValue = tileSize.getStringValue();
            if (stringValue!= null && stringValue.contains(":")) {
                String[] tileSizes = stringValue.split(":");
                if (tileSizes.length == 2) {
                    try {
                        int tHeight = Integer.parseInt(tileSizes[0]);
                        int tWidth = Integer.parseInt(tileSizes[1]);
                        product.setPreferredTileSize(tWidth, tHeight);
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        return product;
    }

    @Override
    public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
        NFileWriteable writeable = ctx.getNetcdfFileWriteable();
        writeable.addGlobalAttribute("Conventions", "CF-1.4");
        if (CfGeocodingPart.isGeographicCRS(product.getSceneGeoCoding())) {
            writeDimensions(writeable, product, "lat", "lon");
        } else {
            writeDimensions(writeable, product, "y", "x");
        }
        Dimension tileSize = ImageManager.getPreferredTileSize(product);
        writeable.addGlobalAttribute("TileSize", tileSize.height + ":" + tileSize.width);
    }

    private void writeDimensions(NFileWriteable writeable, Product p, String dimY, String dimX) throws IOException {
        writeable.addDimension(dimY, p.getSceneRasterHeight());
        writeable.addDimension(dimX, p.getSceneRasterWidth());
    }

    public String readProductType(final ProfileReadContext ctx) {
        Attribute productType = ctx.getNetcdfFile().findGlobalAttribute("Conventions");
        if (productType != null && StringUtils.isNotNullAndNotEmpty(productType.getStringValue())) {
            return  productType.getStringValue();
        } else {
            return Constants.FORMAT_NAME;
        }
    }
}
