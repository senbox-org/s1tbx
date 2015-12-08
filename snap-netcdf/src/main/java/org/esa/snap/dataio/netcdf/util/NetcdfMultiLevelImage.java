/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de) 
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

package org.esa.snap.dataio.netcdf.util;

import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.image.RenderedImage;


public class NetcdfMultiLevelImage extends AbstractNetcdfMultiLevelImage {

    private final Variable variable;
    private final int[] imageOrigin;
    private final ProfileReadContext ctx;

    /**
     * Constructs a new {@code NetcdfMultiLevelImage}.
     *
     * @param rdn      the raster data node
     * @param variable the netcdf variable
     * @param ctx      the context
     */
    public NetcdfMultiLevelImage(RasterDataNode rdn, Variable variable, ProfileReadContext ctx) {
        this(rdn, variable, new int[0], ctx);
    }

    /**
     * Constructs a new {@code NetcdfMultiLevelImage}.
     *
     * @param rdn         the raster data node
     * @param variable    the netcdf variable
     * @param imageOrigin the index within a multidimensional image dataset
     * @param ctx         the context
     */
    public NetcdfMultiLevelImage(RasterDataNode rdn, Variable variable, int[] imageOrigin, ProfileReadContext ctx) {
        super(rdn);
        this.variable = variable;
        this.imageOrigin = imageOrigin.clone();
        this.ctx = ctx;
    }

    @Override
    protected RenderedImage createImage(int level) {
        RasterDataNode rdn = getRasterDataNode();
        NetcdfFile lock = ctx.getNetcdfFile();
        final Object object = ctx.getProperty(Constants.Y_FLIPPED_PROPERTY_NAME);
        boolean isYFlipped = object instanceof Boolean && (Boolean) object;
        int dataBufferType = ImageManager.getDataBufferType(rdn.getDataType());
        int sceneRasterWidth = rdn.getRasterWidth();
        int sceneRasterHeight = rdn.getRasterHeight();
        ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), level);
        Dimension imageTileSize = new Dimension(getTileWidth(), getTileHeight());

        if (variable.getDataType() == DataType.LONG) {
            if (rdn.getName().endsWith("_lsb")) {
                return NetcdfOpImage.createLsbImage(variable, imageOrigin, isYFlipped, lock, dataBufferType,
                                                    sceneRasterWidth, sceneRasterHeight, imageTileSize, resolutionLevel);
            } else {
                return NetcdfOpImage.createMsbImage(variable, imageOrigin, isYFlipped, lock, dataBufferType,
                                                    sceneRasterWidth, sceneRasterHeight, imageTileSize, resolutionLevel);
            }
        } else {
            return new NetcdfOpImage(variable, imageOrigin, isYFlipped, lock,
                                     dataBufferType, sceneRasterWidth, sceneRasterHeight, imageTileSize, resolutionLevel);
        }
    }
}
