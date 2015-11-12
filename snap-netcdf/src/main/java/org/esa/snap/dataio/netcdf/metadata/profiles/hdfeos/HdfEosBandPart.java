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

package org.esa.snap.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.snap.dataio.netcdf.util.AbstractNetcdfMultiLevelImage;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.esa.snap.dataio.netcdf.util.NetcdfMultiLevelImage;
import org.esa.snap.dataio.netcdf.util.NetcdfOpImage;
import org.esa.snap.dataio.netcdf.util.RasterDigest;
import org.esa.snap.dataio.netcdf.util.ScaledVariable;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.media.jai.Interpolation;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.IOException;


public class HdfEosBandPart extends ProfilePartIO {

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        RasterDigest rasterDigest = ctx.getRasterDigest();
        final Variable[] variables = rasterDigest.getRasterVariables();
        for (Variable variable : variables) {
            final int rasterDataType = DataTypeUtils.getRasterDataType(variable);
            final Band band = p.addBand(variable.getShortName(), rasterDataType);
            CfBandPart.readCfBandAttributes(variable, band);
            band.setSourceImage(new NetcdfMultiLevelImage(band, variable, ctx));
        }
        ScaledVariable[] scaledVariables = rasterDigest.getScaledVariables();
        for (ScaledVariable scaledVariable : scaledVariables) {
            Variable variable = scaledVariable.getVariable();
            final int rasterDataType = DataTypeUtils.getRasterDataType(variable);
            final Band band = p.addBand(variable.getShortName(), rasterDataType);
            CfBandPart.readCfBandAttributes(variable, band);
            band.setSourceImage(new ScaledMultiLevelImage(band, scaledVariable, ctx));
        }
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        throw new IllegalStateException();
    }

    private static class ScaledMultiLevelImage extends AbstractNetcdfMultiLevelImage {

        private final Variable variable;
        private final float scaleFactor;
        private final int[] imageOrigin;
        private final ProfileReadContext ctx;

        public ScaledMultiLevelImage(RasterDataNode rdn, ScaledVariable scaledVariable, ProfileReadContext ctx) {
            super(rdn);
            this.variable = scaledVariable.getVariable();
            this.scaleFactor = scaledVariable.getScaleFactor();
            this.imageOrigin = new int[0];
            this.ctx = ctx;
        }

        @Override
        protected RenderedImage createImage(int level) {
            RasterDataNode rdn = getRasterDataNode();
            NetcdfFile lock = ctx.getNetcdfFile();
            final Object object = ctx.getProperty(Constants.Y_FLIPPED_PROPERTY_NAME);
            boolean isYFlipped = object instanceof Boolean && (Boolean) object;
            int dataBufferType = ImageManager.getDataBufferType(rdn.getDataType());
            int sourceWidth = (int) (rdn.getRasterWidth() / scaleFactor);
            int sourceHeight = (int) (rdn.getRasterHeight() / scaleFactor);
            ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), level);
            Dimension imageTileSize = new Dimension(getTileWidth(), getTileHeight());

            RenderedImage netcdfImg;
            if (variable.getDataType() == DataType.LONG) {
                if (rdn.getName().endsWith("_lsb")) {
                    netcdfImg = NetcdfOpImage.createLsbImage(variable, imageOrigin, isYFlipped, lock, dataBufferType,
                                                             sourceWidth, sourceHeight, imageTileSize, resolutionLevel);
                } else {
                    netcdfImg = NetcdfOpImage.createMsbImage(variable, imageOrigin, isYFlipped, lock, dataBufferType,
                                                             sourceWidth, sourceHeight, imageTileSize, resolutionLevel);
                }
            } else {
                netcdfImg = new NetcdfOpImage(variable, imageOrigin, isYFlipped, lock,
                                              dataBufferType, sourceWidth, sourceHeight, imageTileSize, resolutionLevel);
            }

            return ScaleDescriptor.create(netcdfImg, scaleFactor, scaleFactor, 0.5f, 0.5f,
                                                          Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
        }
    }
}
