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

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;

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
        final GeoPos gp0 = p.getGeoCoding().getGeoPos(new PixelPos(0.5f, 0.5f), null);
        final GeoPos gp1 = p.getGeoCoding().getGeoPos(new PixelPos(0.5f, 1.5f), null);
        boolean doFlip = false;
        if (gp1.lat - gp0.lat < 0) {
            doFlip = true;
        }
        for (TiePointGrid tiePointGrid : p.getTiePointGrids()) {
            try {
                final int h = tiePointGrid.getSceneRasterHeight();
                final int w = tiePointGrid.getSceneRasterWidth();
                final int[] shape = new int[]{h, w};
                final Object data = tiePointGrid.getSourceImage().getData().getDataElements(0, 0, w, h, null);
                Array values = Array.factory(DataType.FLOAT, shape, data);
                if (doFlip) {
                    values = values.flip(0);   // flip vertically
                }
                String variableName = ReaderUtils.getVariableName(tiePointGrid);
                ctx.getNetcdfFileWriteable().write(variableName, values);
            } catch (InvalidRangeException ignored) {
                throw new ProductIOException("TiePointData not in the expected range");
            }
        }
    }
}
