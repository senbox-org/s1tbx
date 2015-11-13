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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;

import java.io.IOException;

public class CfTiePointGridPart extends ProfilePartIO {

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        // do nothing
        // tie points are decode as bands by CfBandPart
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        CfBandPart.defineRasterDataNodes(ctx, p.getTiePointGrids());
    }

    @Override
    public void encode(ProfileWriteContext ctx, Product p) throws IOException {
        boolean doFlip = getYFlippedProperty(ctx);

        for (TiePointGrid tiePointGrid : p.getTiePointGrids()) {
            final int h = tiePointGrid.getRasterHeight();
            final int w = tiePointGrid.getRasterWidth();
            final Object data = tiePointGrid.getSourceImage().getData().getDataElements(0, 0, w, h, null);
            ProductData productData = ProductData.createInstance(ProductData.TYPE_FLOAT32, data);
            String variableName = ReaderUtils.getVariableName(tiePointGrid);
            ctx.getNetcdfFileWriteable().findVariable(variableName).write(0, 0, w, h, doFlip, productData);
        }
    }

    private boolean getYFlippedProperty(ProfileWriteContext ctx) {
        Object yFlippedProperty = ctx.getProperty(Constants.Y_FLIPPED_PROPERTY_NAME);
        if (yFlippedProperty instanceof Boolean) {
            return (Boolean) yFlippedProperty;
        }
        return false;
    }

}
