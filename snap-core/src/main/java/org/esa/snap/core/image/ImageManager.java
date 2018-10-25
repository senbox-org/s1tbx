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

package org.esa.snap.core.image;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.jai.operator.PaintDescriptor;
import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RGBChannelDef;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.IntMap;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.runtime.Config;

import javax.media.jai.Histogram;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandMergeDescriptor;
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.ClampDescriptor;
import javax.media.jai.operator.CompositeDescriptor;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.media.jai.operator.InvertDescriptor;
import javax.media.jai.operator.LookupDescriptor;
import javax.media.jai.operator.MatchCDFDescriptor;
import javax.media.jai.operator.MaxDescriptor;
import javax.media.jai.operator.MinDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import javax.media.jai.operator.RescaleDescriptor;
import javax.media.jai.operator.SubtractFromConstDescriptor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * This class provides most of the new imaging features introduced in BEAM 4.5.
 * <p><i>WARNING:</i> Although {@code ImageManager} is intended to belong to the public BEAM API you should use it
 * with care, since it is still under development and may change slightly in forthcoming versions.
 */
public class ImageManager {

    private static final boolean CACHE_INTERMEDIATE_TILES = Config.instance().preferences().getBoolean("snap.enableIntermediateTileCaching", false);


    public static ImageManager getInstance() {
        return Holder.instance;
    }

    public ImageManager() {
    }

    public PlanarImage getSourceImage(RasterDataNode rasterDataNode, int level) {
        return getLevelImage(rasterDataNode.getSourceImage(), level);
    }


    public PlanarImage getValidMaskImage(final RasterDataNode rasterDataNode, int level) {
        if (rasterDataNode.isValidMaskUsed()) {
            return getLevelImage(rasterDataNode.getValidMaskImage(), level);
        }
        return null;
    }

    public PlanarImage getGeophysicalImage(RasterDataNode rasterDataNode, int level) {
        return getLevelImage(rasterDataNode.getGeophysicalImage(), level);
    }

    public static ImageLayout createSingleBandedImageLayout(RasterDataNode rasterDataNode) {
        return createSingleBandedImageLayout(rasterDataNode,
                                             getDataBufferType(rasterDataNode.getDataType()));
    }

    public static ImageLayout createSingleBandedImageLayout(RasterDataNode rasterDataNode, int dataBufferType) {
        int width = rasterDataNode.getRasterWidth();
        int height = rasterDataNode.getRasterHeight();
        Dimension tileSize = getPreferredTileSize(rasterDataNode.getProduct());
        return createSingleBandedImageLayout(dataBufferType, width, height, tileSize.width, tileSize.height);
    }

    public static ImageLayout createSingleBandedImageLayout(int dataBufferType,
                                                            int width,
                                                            int height,
                                                            int tileWidth,
                                                            int tileHeight) {
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataBufferType, tileWidth, tileHeight);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        return new ImageLayout(0, 0,
                               width,
                               height,
                               0, 0,
                               tileWidth,
                               tileHeight,
                               sampleModel,
                               colorModel);
    }

    public static ImageLayout createSingleBandedImageLayout(int dataBufferType,
                                                            int sourceWidth,
                                                            int sourceHeight,
                                                            Dimension tileSize,
                                                            ResolutionLevel level) {
        return createSingleBandedImageLayout(dataBufferType,
                                             null,
                                             sourceWidth,
                                             sourceHeight,
                                             tileSize,
                                             level);
    }

    public static ImageLayout createSingleBandedImageLayout(int dataBufferType,
                                                            Point sourcePos,
                                                            int sourceWidth,
                                                            int sourceHeight,
                                                            Dimension tileSize,
                                                            ResolutionLevel level) {
        if (sourceWidth < 0) {
            throw new IllegalArgumentException("sourceWidth");
        }
        if (sourceHeight < 0) {
            throw new IllegalArgumentException("sourceHeight");
        }
        Assert.notNull("level");
        /*
        final Rectangle destRectangle = AbstractMultiLevelSource.getImageRectangle(sourcePos != null ? sourcePos.x : 0,
                                                                                   sourcePos != null ? sourcePos.y : 0,
                                                                                   sourceWidth,
                                                                                   sourceHeight,
                                                                                   level.getScale());
                                                                                   */

        Rectangle sourceBounds = new Rectangle(sourcePos != null ? sourcePos.x : 0,
                                               sourcePos != null ? sourcePos.y : 0,
                                               sourceWidth,
                                               sourceHeight);
        final Rectangle destBounds = DefaultMultiLevelSource.getLevelImageBounds(sourceBounds, level.getScale());

        final int destWidth = destBounds.width;
        final int destHeight = destBounds.height;
        tileSize = tileSize != null ? tileSize : JAIUtils.computePreferredTileSize(destWidth, destHeight, 1);
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataBufferType,
                                                                           tileSize.width, tileSize.height);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

        if (colorModel == null) {
            final int dataType = sampleModel.getDataType();
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            int[] nBits = {DataBuffer.getDataTypeSize(dataType)};
            colorModel = new ComponentColorModel(cs, nBits, false, true,
                                                 Transparency.OPAQUE,
                                                 dataType);
        }

        return new ImageLayout(destBounds.x,
                               destBounds.y,
                               destWidth,
                               destHeight,
                               0, 0,
                               tileSize.width,
                               tileSize.height,
                               sampleModel,
                               colorModel);
    }

    public static int getDataBufferType(int productDataType) {
        switch (productDataType) {
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
                return DataBuffer.TYPE_BYTE;
            case ProductData.TYPE_INT16:
                return DataBuffer.TYPE_SHORT;
            case ProductData.TYPE_UINT16:
                return DataBuffer.TYPE_USHORT;
            case ProductData.TYPE_INT32:
            case ProductData.TYPE_UINT32:
                return DataBuffer.TYPE_INT;
            case ProductData.TYPE_FLOAT32:
                return DataBuffer.TYPE_FLOAT;
            case ProductData.TYPE_FLOAT64:
                return DataBuffer.TYPE_DOUBLE;
            default:
                throw new IllegalArgumentException("productDataType");
        }
    }

    public static int getProductDataType(int dataBufferType) {
        switch (dataBufferType) {
            case DataBuffer.TYPE_BYTE:
                return ProductData.TYPE_UINT8;
            case DataBuffer.TYPE_SHORT:
                return ProductData.TYPE_INT16;
            case DataBuffer.TYPE_USHORT:
                return ProductData.TYPE_UINT16;
            case DataBuffer.TYPE_INT:
                return ProductData.TYPE_INT32;
            case DataBuffer.TYPE_FLOAT:
                return ProductData.TYPE_FLOAT32;
            case DataBuffer.TYPE_DOUBLE:
                return ProductData.TYPE_FLOAT64;
            default:
                throw new IllegalArgumentException("dataBufferType");
        }
    }

    public static Dimension getPreferredTileSize(Product product) {
        Dimension tileSize;
        final Dimension preferredTileSize = product.getPreferredTileSize();
        if (preferredTileSize != null) {
            tileSize = preferredTileSize;
        } else {
            tileSize = JAIUtils.computePreferredTileSize(product.getSceneRasterWidth(),
                                                         product.getSceneRasterHeight(), 1);
        }
        return tileSize;
    }

    public RenderedImage createColoredBandImage(RasterDataNode[] rasterDataNodes,
                                                ImageInfo imageInfo,
                                                int level) {
        Assert.notNull(rasterDataNodes, "rasterDataNodes");
        Assert.state(rasterDataNodes.length == 1
                             || rasterDataNodes.length == 3
                             || rasterDataNodes.length == 4,
                     "invalid number of bands"
        );

        prepareImageInfos(rasterDataNodes, ProgressMonitor.NULL);
        if (rasterDataNodes.length == 1) {
            return createColored1BandImage(rasterDataNodes[0], imageInfo, level);
        } else {
            return createColored3BandImage(rasterDataNodes, imageInfo, level);
        }
    }

    private PlanarImage createColored1BandImage(RasterDataNode valueBand, ImageInfo valueImageInfo, int level) {
        Assert.notNull(valueBand, "valueBand");
        if (valueImageInfo == null) {
            valueImageInfo = valueBand.getImageInfo(ProgressMonitor.NULL); // CP_V
        }
        Assert.notNull(valueImageInfo, "valueImageInfo");
        PlanarImage sourceImage = getSourceImage(valueBand, level); // V
        PlanarImage valueValidMaskImage = getValidMaskImage(valueBand, level); // VM
        PlanarImage valueImage = createByteIndexedImage(valueBand, sourceImage, valueImageInfo);
        valueImage = createMatchCdfImage(valueImage, valueImageInfo.getHistogramMatching(), new Stx[]{valueBand.getStx()});
        valueImage = createLookupRgbImage(valueBand, valueImage, valueImageInfo);
        RasterDataNode uncertaintyBand = getUncertaintyBand(valueBand);
        if (uncertaintyBand != null) {
            ImageInfo uncertaintyImageInfo = uncertaintyBand.getImageInfo(ProgressMonitor.NULL); // CP_U,
            ImageInfo.UncertaintyVisualisationMode visualisationMode = uncertaintyImageInfo.getUncertaintyVisualisationMode();
            if (visualisationMode != ImageInfo.UncertaintyVisualisationMode.None) {
                PlanarImage uncertaintySourceImage = getSourceImage(uncertaintyBand, level);  // U
                PlanarImage uncertaintyValidMaskImage = getValidMaskImage(uncertaintyBand, level); // UM
                if (visualisationMode == ImageInfo.UncertaintyVisualisationMode.Transparency_Blending) {
                    PlanarImage confidenceImage = createByteIndexedImage(uncertaintyBand, uncertaintySourceImage, uncertaintyImageInfo, true);
                    valueImage = paint(valueImage,
                                       confidenceImage, new Color(0, 0, 0, 0),
                                       valueValidMaskImage, valueImageInfo.getNoDataColor(),
                                       uncertaintyValidMaskImage, uncertaintyImageInfo.getNoDataColor());
                } else if (visualisationMode == ImageInfo.UncertaintyVisualisationMode.Monochromatic_Blending) {
                    PlanarImage confidenceImage = createByteIndexedImage(uncertaintyBand, uncertaintySourceImage, uncertaintyImageInfo, true);
                    valueImage = paint(valueImage,
                                       confidenceImage, uncertaintyImageInfo.getColorPaletteDef().getLastPoint().getColor(),
                                       valueValidMaskImage, valueImageInfo.getNoDataColor(),
                                       uncertaintyValidMaskImage, uncertaintyImageInfo.getNoDataColor());
                } else if (visualisationMode == ImageInfo.UncertaintyVisualisationMode.Polychromatic_Blending) {
                    PlanarImage confidenceImage = createByteIndexedImage(uncertaintyBand, uncertaintySourceImage, uncertaintyImageInfo, true);
                    //PlanarImage distrustImage = createByteIndexedImage(uncertaintyBand, uncertaintySourceImage, uncertaintyImageInfo, false);
                    PlanarImage distrustImage = SubtractFromConstDescriptor.create(confidenceImage, new double[]{255}, createDefaultRenderingHints(confidenceImage, null));
                    PlanarImage uncertaintyImage = createLookupRgbImage(uncertaintyBand, distrustImage, uncertaintyImageInfo);
                    valueImage = paint(valueImage, confidenceImage, uncertaintyImage);
                    valueImage = paint(valueImage,
                                       valueValidMaskImage, valueImageInfo.getNoDataColor(),
                                       uncertaintyValidMaskImage, uncertaintyImageInfo.getNoDataColor());
                } else if (visualisationMode == ImageInfo.UncertaintyVisualisationMode.Polychromatic_Overlay) {
                    PlanarImage distrustImage = createByteIndexedImage(uncertaintyBand, uncertaintySourceImage, uncertaintyImageInfo, false);
                    PlanarImage uncertaintyImage = createLookupRgbImage(uncertaintyBand, distrustImage, uncertaintyImageInfo);
                    valueImage = paint(valueImage, 0.5, uncertaintyImage);
                    valueImage = paint(valueImage,
                                       valueValidMaskImage, valueImageInfo.getNoDataColor(),
                                       uncertaintyValidMaskImage, uncertaintyImageInfo.getNoDataColor());
                } else {
                    Assert.state(false, "unknown uncertainty visualisation mode " + visualisationMode);
                }
                return valueImage;
            }
        }
        if (valueValidMaskImage != null) {
            valueImage = paint(valueImage, valueValidMaskImage, valueImageInfo.getNoDataColor());
        }
        return valueImage;
    }

    /**
     * <p><b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
     * change significantly before reaching stability. It is being made available at this early stage to solicit
     * feedback from pioneering adopters on the understanding that any code that uses this API will almost certainly
     * be broken (repeatedly) as the API evolves.
     */
    public static RasterDataNode getUncertaintyBand(RasterDataNode valueBand) {
        final String[] roleNames = {"uncertainty", "error", "variance", "standard_deviation", "confidence"};
        RasterDataNode uncertaintyBand = null;
        for (String roleName : roleNames) {
            uncertaintyBand = valueBand.getAncillaryVariable(roleName);
            if (uncertaintyBand != null) {
                break;
            }
        }
        return uncertaintyBand;
    }

    private static PlanarImage paint(PlanarImage sourceImage,
                                     PlanarImage maskImage, Color paintColor) {
        RenderingHints renderingHints = createDefaultRenderingHints(sourceImage, null);
        if (maskImage != null) {
            sourceImage = PaintDescriptor.create(sourceImage, maskImage, paintColor, false, renderingHints);
        }
        return sourceImage;
    }

    private static PlanarImage paint(PlanarImage sourceImage,
                                     PlanarImage maskImage1, Color paintColor1,
                                     PlanarImage maskImage2, Color paintColor2) {
        RenderingHints renderingHints = createDefaultRenderingHints(sourceImage, null);
        if (maskImage1 != null) {
            sourceImage = PaintDescriptor.create(sourceImage, maskImage1, paintColor1, false, renderingHints);
        }
        if (maskImage2 != null) {
            sourceImage = PaintDescriptor.create(sourceImage, maskImage2, paintColor2, false, renderingHints);
        }
        return sourceImage;
    }

    private static PlanarImage paint(PlanarImage sourceImage,
                                     PlanarImage maskImage1, Color paintColor1,
                                     PlanarImage maskImage2, Color paintColor2,
                                     PlanarImage maskImage3, Color paintColor3) {
        RenderingHints renderingHints = createDefaultRenderingHints(sourceImage, null);
        if (maskImage1 != null) {
            sourceImage = PaintDescriptor.create(sourceImage, maskImage1, paintColor1, false, renderingHints);
        }
        if (maskImage2 != null) {
            sourceImage = PaintDescriptor.create(sourceImage, maskImage2, paintColor2, false, renderingHints);
        }
        if (maskImage3 != null) {
            sourceImage = PaintDescriptor.create(sourceImage, maskImage3, paintColor3, false, renderingHints);
        }
        return sourceImage;
    }

    private static PlanarImage paint(PlanarImage sourceImage, double transparency, PlanarImage maskColorImage) {
        RenderingHints renderingHints = createDefaultRenderingHints(sourceImage, null);
        RenderedImage maskImage = ConstantDescriptor.create((float) sourceImage.getWidth(),
                                                            (float) sourceImage.getHeight(),
                                                            new Byte[]{(byte) (255 * (1.0 - transparency))},
                                                            renderingHints);
        return paintImpl(sourceImage, maskColorImage, maskImage, renderingHints);
    }

    private static PlanarImage paint(PlanarImage sourceImage, PlanarImage maskImage, PlanarImage maskColorImage) {
        RenderingHints renderingHints = createDefaultRenderingHints(sourceImage, null);
        return paintImpl(sourceImage, maskColorImage, maskImage, renderingHints);
    }

    // Check to write own JAI operator, look at PaintDescriptor which is very similar
    private static PlanarImage paintImpl(PlanarImage sourceImage, PlanarImage maskColorImage, RenderedImage maskImage, RenderingHints renderingHints) {
        boolean targetHasAlpha = sourceImage.getNumBands() == 4 || maskColorImage.getNumBands() == 4;

        if (sourceImage.getNumBands() == 4) {
            RenderedOp alphaImage = BandSelectDescriptor.create(sourceImage, new int[]{3}, renderingHints);
            maskImage = MinDescriptor.create(maskImage, alphaImage, renderingHints);
            sourceImage = BandSelectDescriptor.create(sourceImage, new int[]{0, 1, 2}, renderingHints);
        }

        if (maskColorImage.getNumBands() == 4) {
            RenderedOp alphaImage = BandSelectDescriptor.create(maskColorImage, new int[]{3}, renderingHints);
            maskImage = MinDescriptor.create(maskImage, alphaImage, renderingHints);
            maskColorImage = BandSelectDescriptor.create(maskColorImage, new int[]{0, 1, 2}, renderingHints);
        }

        return CompositeDescriptor.create(sourceImage, maskColorImage,
                                          maskImage, null, false,
                                          targetHasAlpha ? CompositeDescriptor.DESTINATION_ALPHA_LAST : CompositeDescriptor.NO_DESTINATION_ALPHA,
                                          renderingHints);
    }

    private PlanarImage createColored3BandImage(RasterDataNode[] rasters, ImageInfo rgbImageInfo, int level) {
        Assert.notNull(rasters, "rasters");
        Assert.notNull(rgbImageInfo, "rgbImageInfo");
        RenderedImage[] images = new RenderedImage[rasters.length];
        RenderedImage[] validMaskImages = new RenderedImage[rasters.length];
        Stx[] stxs = new Stx[rasters.length];
        for (int i = 0; i < rasters.length; i++) {
            RasterDataNode raster = rasters[i];
            stxs[i] = raster.getStx();
            RenderedImage sourceImage = getSourceImage(raster, level);
            images[i] = createByteIndexedImage(raster,
                                               sourceImage,
                                               rgbImageInfo.getRgbChannelDef().getMinDisplaySample(i),
                                               rgbImageInfo.getRgbChannelDef().getMaxDisplaySample(i),
                                               rgbImageInfo.getRgbChannelDef().getGamma(i));
            validMaskImages[i] = getValidMaskImage(raster, level);
        }
        return createMergeRgbaOp(images, validMaskImages, rgbImageInfo.getHistogramMatching(), stxs);
    }

    private static PlanarImage createByteIndexedImage(RasterDataNode raster,
                                                      RenderedImage sourceImage,
                                                      ImageInfo imageInfo) {
        ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        final double minSample = colorPaletteDef.getMinDisplaySample();
        final double maxSample = colorPaletteDef.getMaxDisplaySample();

        if (isClassificationBand(raster)) {
            final IntMap sampleColorIndexMap = new IntMap((int) minSample - 1, 4098);
            final ColorPaletteDef.Point[] points = colorPaletteDef.getPoints();
            for (int colorIndex = 0; colorIndex < points.length; colorIndex++) {
                sampleColorIndexMap.putValue((int) getSample(points[colorIndex]), colorIndex);
            }
            final int undefinedIndex = colorPaletteDef.getNumPoints();
            return createIndexedImage(sourceImage, sampleColorIndexMap, undefinedIndex);
        } else {
            return createByteIndexedImage(raster, sourceImage, minSample, maxSample, 1.0);
        }
    }

    private PlanarImage createByteIndexedImage(RasterDataNode band, PlanarImage sourceImage, ImageInfo imageInfo, boolean invertRamp) {
        ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        if (invertRamp) {
            return createByteIndexedImage(band, sourceImage, colorPaletteDef.getMaxDisplaySample(), colorPaletteDef.getMinDisplaySample(), 1.0);
        } else {
            return createByteIndexedImage(band, sourceImage, colorPaletteDef.getMinDisplaySample(), colorPaletteDef.getMaxDisplaySample(), 1.0);
        }
    }

    private static boolean isClassificationBand(RasterDataNode raster) {
        return ((raster instanceof Band) ? ((Band) raster).getIndexCoding() : null) != null;
    }

    private static PlanarImage createByteIndexedImage(RasterDataNode raster,
                                                      RenderedImage sourceImage,
                                                      double minSample,
                                                      double maxSample,
                                                      double gamma) {
        double newMin = raster.scaleInverse(minSample);
        double newMax = raster.scaleInverse(maxSample);

        if (mustReinterpretSourceImage(raster, sourceImage)) {
            sourceImage = ReinterpretDescriptor.create(sourceImage, 1.0, 0.0, ReinterpretDescriptor.LINEAR,
                                                       ReinterpretDescriptor.INTERPRET_BYTE_SIGNED, null);
        }

        final boolean logarithmicDisplay = raster.getImageInfo().isLogScaled();
        final boolean rasterIsLog10Scaled = raster.isLog10Scaled();
        if (logarithmicDisplay) {
            if (!rasterIsLog10Scaled) {
                final double offset = raster.scaleInverse(0.0);
                sourceImage = ReinterpretDescriptor.create(sourceImage, 1.0, -offset, ReinterpretDescriptor.LOGARITHMIC,
                                                           ReinterpretDescriptor.AWT, null);
                newMin = Math.log10(newMin - offset);
                newMax = Math.log10(newMax - offset);
            }
        } else {
            if (rasterIsLog10Scaled) {
                sourceImage = ReinterpretDescriptor.create(sourceImage, raster.getScalingFactor(), raster.getScalingOffset(), ReinterpretDescriptor.EXPONENTIAL,
                                                           ReinterpretDescriptor.AWT, null);
                newMin = minSample;
                newMax = maxSample;
            }
        }

        final double factor = 255.0 / (newMax - newMin);
        final double offset = 255.0 * newMin / (newMin - newMax);
        PlanarImage image = createRescaleOp(sourceImage, factor, offset);
        image = createByteFormatOp(image);
        if (gamma != 1.0) {
            byte[] gammaCurve = MathUtils.createGammaCurve(gamma, new byte[256]);
            LookupTableJAI lookupTable = new LookupTableJAI(gammaCurve);
            image = LookupDescriptor.create(image, lookupTable, createDefaultRenderingHints(image, null));
        }
        return image;
    }

    private static boolean mustReinterpretSourceImage(RasterDataNode raster, RenderedImage sourceImage) {
        return sourceImage.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE &&
                raster.getDataType() == ProductData.TYPE_INT8;
    }

    private static RenderingHints createDefaultRenderingHints(RenderedImage sourceImage, ImageLayout targetLayout) {
        Map<RenderingHints.Key, Object> map = new HashMap<>(7);
        if (!CACHE_INTERMEDIATE_TILES) {
            map.put(JAI.KEY_TILE_CACHE, null);
        }
        if (sourceImage != null) {
            if (targetLayout == null) {
                targetLayout = new ImageLayout();
            }
            if (!targetLayout.isValid(ImageLayout.TILE_GRID_X_OFFSET_MASK)) {
                targetLayout.setTileGridXOffset(sourceImage.getTileGridXOffset());
            }
            if (!targetLayout.isValid(ImageLayout.TILE_GRID_Y_OFFSET_MASK)) {
                targetLayout.setTileGridYOffset(sourceImage.getTileGridYOffset());
            }
            if (!targetLayout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
                targetLayout.setTileWidth(sourceImage.getTileWidth());
            }
            if (!targetLayout.isValid(ImageLayout.TILE_HEIGHT_MASK)) {
                targetLayout.setTileHeight(sourceImage.getTileHeight());
            }
            map.put(JAI.KEY_IMAGE_LAYOUT, targetLayout);
        }
        return new RenderingHints(map);
    }

    private static PlanarImage createIndexedImage(RenderedImage sourceImage, IntMap intMap, int undefinedIndex) {
        if (sourceImage.getSampleModel().getNumBands() != 1) {
            throw new IllegalArgumentException();
        }
        final int[][] ranges = intMap.getRanges();
        final int keyMin = ranges[0][0];
        final int keyMax = ranges[0][1];
        final int valueMin = ranges[1][0];
        final int valueMax = ranges[1][1];
        final int keyRange = 1 + keyMax - keyMin;
        final int valueRange = 1 + valueMax - valueMin;
        if (keyRange > Short.MAX_VALUE) {
            throw new IllegalArgumentException("intMap: keyRange > Short.MAX_VALUE");
        }
        LookupTableJAI lookup;
        if (valueRange <= 256) {
            final byte[] table = new byte[keyRange + 2];
            for (int i = 1; i < table.length - 1; i++) {
                final int value = intMap.getValue(keyMin + i - 1);
                table[i] = (byte) (value != IntMap.NULL ? value : undefinedIndex);
            }
            table[0] = (byte) undefinedIndex;
            table[table.length - 1] = (byte) undefinedIndex;
            lookup = new LookupTableJAI(table, keyMin - 1);
        } else if (valueRange <= 65536) {
            final short[] table = new short[keyRange + 2];
            for (int i = 1; i < table.length; i++) {
                final int value = intMap.getValue(keyMin + i - 1);
                table[i] = (short) (value != IntMap.NULL ? value : undefinedIndex);
            }
            table[0] = (short) undefinedIndex;
            table[table.length - 1] = (short) undefinedIndex;
            lookup = new LookupTableJAI(table, keyMin - 1, valueRange > 32767);
        } else {
            final int[] table = new int[keyRange + 2];
            for (int i = 1; i < table.length; i++) {
                final int value = intMap.getValue(keyMin + i - 1);
                table[i] = value != IntMap.NULL ? value : undefinedIndex;
            }
            table[0] = undefinedIndex;
            table[table.length - 1] = undefinedIndex;
            lookup = new LookupTableJAI(table, keyMin - 1);
        }
        RenderingHints hints = createDefaultRenderingHints(sourceImage, null);
        sourceImage = ClampDescriptor.create(sourceImage,
                                             new double[]{keyMin - 1},
                                             new double[]{keyMax + 1},
                                             hints);
        return LookupDescriptor.create(sourceImage, lookup, hints);
    }

    private static PlanarImage createMergeRgbaOp(RenderedImage[] sourceImages,
                                                 RenderedImage[] maskOpImages,
                                                 ImageInfo.HistogramMatching histogramMatching,
                                                 Stx[] stxs) {
        RenderingHints hints = createDefaultRenderingHints(sourceImages[0], null);

        if (histogramMatching == ImageInfo.HistogramMatching.None) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(sourceImages[0]);
            pb.addSource(sourceImages[1]);
            pb.addSource(sourceImages[2]);
            RenderedImage alpha = createMapOp(maskOpImages);
            if (alpha != null) {
                pb.addSource(alpha);
            }
            return JAI.create("bandmerge", pb, hints);
        } else {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(sourceImages[0]);
            pb.addSource(sourceImages[1]);
            pb.addSource(sourceImages[2]);
            PlanarImage image = JAI.create("bandmerge", pb, hints);

            if (histogramMatching == ImageInfo.HistogramMatching.Equalize) {
                image = createMatchCdfEqualizeImage(image, stxs);
            } else {
                image = createMatchCdfNormalizeImage(image, stxs);
            }

            RenderedImage alpha = createMapOp(maskOpImages);
            if (alpha != null) {
                pb = new ParameterBlock();
                pb.addSource(image);
                pb.addSource(alpha);
                image = JAI.create("bandmerge", pb, hints);
            }
            return image;
        }
    }

    private static RenderedImage createMapOp(RenderedImage[] maskOpImages) {
        RenderingHints hints = createDefaultRenderingHints(maskOpImages.length > 0 ? maskOpImages[0] : null, null);
        RenderedImage alpha = null;
        for (RenderedImage maskOpImage : maskOpImages) {
            if (maskOpImage != null) {
                if (alpha != null) {
                    alpha = MaxDescriptor.create(alpha, maskOpImage, hints);
                } else {
                    alpha = maskOpImage;
                }
            }
        }
        return alpha;
    }

    private static PlanarImage createLookupRgbImage(RasterDataNode rasterDataNode, PlanarImage sourceImage, ImageInfo imageInfo) {
        Color[] palette;
        ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        if (isClassificationBand(rasterDataNode)) {
            Color[] origPalette = colorPaletteDef.getColors();
            palette = Arrays.copyOf(origPalette, origPalette.length + 1);
            palette[palette.length - 1] = imageInfo.getNoDataColor();
        } else {
            palette = createColorPalette(imageInfo);
        }

        final byte[][] lutData;
        if (colorPaletteDef.isFullyOpaque()) {
            lutData = new byte[3][palette.length];
            for (int i = 0; i < palette.length; i++) {
                lutData[0][i] = (byte) palette[i].getRed();
                lutData[1][i] = (byte) palette[i].getGreen();
                lutData[2][i] = (byte) palette[i].getBlue();
            }
        } else {
            lutData = new byte[4][palette.length];
            for (int i = 0; i < palette.length; i++) {
                lutData[0][i] = (byte) palette[i].getRed();
                lutData[1][i] = (byte) palette[i].getGreen();
                lutData[2][i] = (byte) palette[i].getBlue();
                lutData[3][i] = (byte) palette[i].getAlpha();
            }
        }
        return createLookupOp(sourceImage, lutData);
    }

    private static PlanarImage createMatchCdfImage(PlanarImage sourceImage,
                                                   ImageInfo.HistogramMatching histogramMatching, Stx[] stxs) {
        final boolean doEqualize = ImageInfo.HistogramMatching.Equalize == histogramMatching;
        final boolean doNormalize = ImageInfo.HistogramMatching.Normalize == histogramMatching;
        if (doEqualize) {
            sourceImage = createMatchCdfEqualizeImage(sourceImage, stxs);
        } else if (doNormalize) {
            sourceImage = createMatchCdfNormalizeImage(sourceImage, stxs);
        }
        return sourceImage;
    }

    private static PlanarImage createMatchCdfEqualizeImage(PlanarImage sourceImage, Stx[] stxs) {

        Assert.notNull(sourceImage, "sourceImage");
        Assert.notNull(stxs, "stxs");
        int numBands = sourceImage.getSampleModel().getNumBands();
        Assert.argument(stxs.length == numBands, "stxs");

        final Histogram histogram = createHistogram(sourceImage, stxs);

        // Create an equalization CDF.
        float[][] eqCDF = new float[numBands][];
        for (int b = 0; b < numBands; b++) {
            int binCount = histogram.getNumBins(b);
            eqCDF[b] = new float[binCount];
            for (int i = 0; i < binCount; i++) {
                eqCDF[b][i] = (float) (i + 1) / (float) binCount;
            }
        }
        return MatchCDFDescriptor.create(sourceImage, eqCDF, createDefaultRenderingHints(sourceImage, null));
    }

    private static Histogram createHistogram(PlanarImage sourceImage, Stx[] stxs) {
        final Histogram histogram = createHistogram(stxs);
        sourceImage.setProperty("histogram", histogram);
        if (sourceImage instanceof RenderedOp) {
            RenderedOp renderedOp = (RenderedOp) sourceImage;
            renderedOp.getRendering().setProperty("histogram", histogram);
        }
        return histogram;
    }

    private static Histogram createHistogram(Stx[] stxs) {
        Histogram histogram = new Histogram(stxs[0].getHistogramBinCount(), 0, 256, stxs.length);
        for (int i = 0; i < stxs.length; i++) {
            System.arraycopy(stxs[i].getHistogramBins(), 0, histogram.getBins(i), 0, stxs[0].getHistogramBinCount());
        }
        return histogram;
    }

    private static PlanarImage createMatchCdfNormalizeImage(PlanarImage sourceImage, Stx[] stxs) {
        final double dev = 256.0;
        int numBands = sourceImage.getSampleModel().getNumBands();
        final double[] means = new double[numBands];
        Arrays.fill(means, 0.5 * dev);
        final double[] stdDevs = new double[numBands];
        Arrays.fill(stdDevs, 0.25 * dev);
        return createHistogramNormalizedImage(sourceImage, stxs, means, stdDevs);
    }

    private static PlanarImage createHistogramNormalizedImage(PlanarImage sourceImage, Stx[] stxs, double[] mean,
                                                              double[] stdDev) {
        int numBands = sourceImage.getSampleModel().getNumBands();
        Assert.argument(numBands == mean.length, "length of mean must be equal to number of bands in the image");
        Assert.argument(numBands == stdDev.length, "length of stdDev must be equal to number of bands in the image");

        final Histogram histogram = createHistogram(sourceImage, stxs);

        float[][] normCDF = new float[numBands][];
        for (int b = 0; b < numBands; b++) {
            int binCount = histogram.getNumBins(b);
            normCDF[b] = new float[binCount];
            double mu = mean[b];
            double twoSigmaSquared = 2.0 * stdDev[b] * stdDev[b];
            normCDF[b][0] = (float) Math.exp(-mu * mu / twoSigmaSquared);
            for (int i = 1; i < binCount; i++) {
                double deviation = i - mu;
                normCDF[b][i] = normCDF[b][i - 1] +
                        (float) Math.exp(-deviation * deviation / twoSigmaSquared);
            }
        }

        for (int b = 0; b < numBands; b++) {
            int binCount = histogram.getNumBins(b);
            double CDFnormLast = normCDF[b][binCount - 1];
            for (int i = 0; i < binCount; i++) {
                normCDF[b][i] /= CDFnormLast;
            }
        }

        return MatchCDFDescriptor.create(sourceImage, normCDF, createDefaultRenderingHints(sourceImage, null));
    }

    private static PlanarImage getLevelImage(MultiLevelImage levelZeroImage, int level) {
        RenderedImage image = levelZeroImage.getImage(level);
        return PlanarImage.wrapRenderedImage(image);
    }

    public ImageInfo getImageInfo(RasterDataNode[] rasters) {
        Assert.notNull(rasters, "rasters");
        Assert.argument(rasters.length == 1 || rasters.length == 3, "rasters.length == 1 || rasters.length == 3");
        if (rasters.length == 1) {
            RasterDataNode raster = rasters[0];
            Assert.state(raster.getImageInfo() != null, "raster.getImageInfo() != null");
            return raster.getImageInfo();
        } else {
            final RGBChannelDef rgbChannelDef = new RGBChannelDef();
            for (int i = 0; i < rasters.length; i++) {
                RasterDataNode raster = rasters[i];
                Assert.state(rasters[i].getImageInfo() != null, "rasters[i].getImageInfo() != null");
                ImageInfo imageInfo = raster.getImageInfo();
                rgbChannelDef.setSourceName(i, raster.getName());
                rgbChannelDef.setMinDisplaySample(i, imageInfo.getColorPaletteDef().getMinDisplaySample());
                rgbChannelDef.setMaxDisplaySample(i, imageInfo.getColorPaletteDef().getMaxDisplaySample());
            }
            return new ImageInfo(rgbChannelDef);
        }
    }

    /*
     * Non-API.
     */
    public void prepareImageInfos(RasterDataNode[] rasterDataNodes, ProgressMonitor pm) {
        int numTaskSteps = 0;
        for (RasterDataNode raster : rasterDataNodes) {
            numTaskSteps += raster.getImageInfo() == null ? 1 : 0;
        }

        pm.beginTask("Computing image statistics", numTaskSteps);
        try {
            for (final RasterDataNode raster : rasterDataNodes) {
                final ImageInfo imageInfo = raster.getImageInfo();
                if (imageInfo == null) {
                    raster.getImageInfo(SubProgressMonitor.create(pm, 1));
                }
            }
        } finally {
            pm.done();
        }
    }


    /*
     * Non-API.
     */
    public int getStatisticsLevel(RasterDataNode raster, int levelCount) {
        final long imageSize = (long) raster.getRasterWidth() * raster.getRasterHeight();
        final int statisticsLevel;
        if (imageSize <= DefaultMultiLevelModel.DEFAULT_MAX_LEVEL_PIXEL_COUNT) {
            statisticsLevel = 0;
        } else {
            statisticsLevel = levelCount - 1;
        }
        return statisticsLevel;
    }

    public PlanarImage createColoredMaskImage(Product product, String expression, Color color, boolean invertMask,
                                              int level) {
        // todo - [multisize_products] fix: rasterDataNode arg is null --> default product scene raster image layout will be used!! (nf)
        MultiLevelImage maskImage = product.getMaskImage(expression, null);
        RenderedImage levelImage = maskImage.getImage(level);
        return createColoredMaskImage(color, levelImage, invertMask);
    }

    public static PlanarImage createColoredMaskImage(Color color, RenderedImage alphaImage, boolean invertAlpha) {
        RenderingHints hints = createDefaultRenderingHints(alphaImage, null);
        return createColoredMaskImage(color, invertAlpha ? InvertDescriptor.create(alphaImage, hints) : alphaImage,
                                      hints);
    }

    public static PlanarImage createColoredMaskImage(RenderedImage maskImage, Color color, double opacity) {
        RenderingHints hints = createDefaultRenderingHints(maskImage, null);
        RenderedImage alphaImage = MultiplyConstDescriptor.create(maskImage, new double[]{opacity}, hints);
        return createColoredMaskImage(color, alphaImage, hints);
    }

    public static PlanarImage createColoredMaskImage(Color color, RenderedImage alphaImage, RenderingHints hints) {
        RenderedOp colorImage =
                ConstantDescriptor.create(
                        (float) alphaImage.getWidth(),
                        (float) alphaImage.getHeight(),
                        new Byte[]{
                                (byte) color.getRed(),
                                (byte) color.getGreen(),
                                (byte) color.getBlue(),
                        }, hints
                );
        return BandMergeDescriptor.create(colorImage, alphaImage, hints);
    }

    public static MultiLevelImage createMaskedGeophysicalImage(final RasterDataNode node, Number maskValue) {
        MultiLevelImage varImage = node.getGeophysicalImage();
        if (node.getValidPixelExpression() != null) {
            varImage = replaceInvalidValues(node, varImage, node.getValidMaskImage(), maskValue);
        } else if (node.isNoDataValueSet() && node.isNoDataValueUsed() && Double.compare(maskValue.doubleValue(), node.getGeophysicalNoDataValue()) != 0) {
            varImage = replaceNoDataValue(node, varImage, node.getGeophysicalNoDataValue(), maskValue);
        }
        return varImage;
    }

    private static MultiLevelImage replaceInvalidValues(final RasterDataNode rasterDataNode, final MultiLevelImage srcImage,
                                                        final MultiLevelImage maskImage, final Number fillValue) {

        final MultiLevelModel multiLevelModel = rasterDataNode.getMultiLevelModel();
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(multiLevelModel) {

            @Override
            public RenderedImage createImage(int sourceLevel) {
                return new FillConstantOpImage(srcImage.getImage(sourceLevel), maskImage.getImage(sourceLevel), fillValue);
            }
        });
    }

    private static MultiLevelImage replaceNoDataValue(final RasterDataNode rasterDataNode, final MultiLevelImage srcImage,
                                                      final double noDataValue, final Number newValue) {

        final MultiLevelModel multiLevelModel = rasterDataNode.getMultiLevelModel();
        final int targetDataType = ImageManager.getDataBufferType(rasterDataNode.getGeophysicalDataType());
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(multiLevelModel) {

            @Override
            public RenderedImage createImage(int sourceLevel) {
                return new ReplaceValueOpImage(srcImage.getImage(sourceLevel), noDataValue, newValue, targetDataType);
            }
        });
    }

    public static RenderedImage createFormatOp(RenderedImage image, int dataType) {
        if (image.getSampleModel().getDataType() == dataType) {
            return PlanarImage.wrapRenderedImage(image);
        }
        return FormatDescriptor.create(image,
                                       dataType,
                                       createDefaultRenderingHints(image, null));
    }

    /**
     * @deprecated since SNAP 2, use {@link Product#findImageToModelTransform(GeoCoding)}
     */
    @Deprecated
    public static AffineTransform getImageToModelTransform(GeoCoding geoCoding) {
        return Product.findImageToModelTransform(geoCoding);
    }


    private static PlanarImage createRescaleOp(RenderedImage src, double factor, double offset) {
        if (factor == 1.0 && offset == 0.0) {
            return PlanarImage.wrapRenderedImage(src);
        }
        return RescaleDescriptor.create(src,
                                        new double[]{factor},
                                        new double[]{offset},
                                        createDefaultRenderingHints(src, null));
    }

    private static PlanarImage createLookupOp(RenderedImage src, byte[][] lookupTable) {
        LookupTableJAI lookup = new LookupTableJAI(lookupTable);
        return LookupDescriptor.create(src, lookup, createDefaultRenderingHints(src, null));
    }

    private static PlanarImage createByteFormatOp(RenderedImage src) {
        ColorModel cm = ImageUtils.create8BitGreyscaleColorModel();
        SampleModel sm = cm.createCompatibleSampleModel(src.getTileWidth(), src.getTileHeight());
        ImageLayout layout = new ImageLayout(src);
        layout.setColorModel(cm);
        layout.setSampleModel(sm);
        return FormatDescriptor.create(src, DataBuffer.TYPE_BYTE, createDefaultRenderingHints(src, layout));
    }

    public static Color[] createColorPalette(ImageInfo imageInfo) {
        Debug.assertNotNull(imageInfo);
        final boolean logScaled = imageInfo.isLogScaled();
        final ColorPaletteDef cpd = imageInfo.getColorPaletteDef();
        Debug.assertNotNull(cpd);
        Debug.assertTrue(cpd.getNumPoints() >= 2);

        final int numColors = cpd.getNumColors();
        final Color[] colorPalette = new Color[numColors];

        ImageInfo.UncertaintyVisualisationMode uvMode = imageInfo.getUncertaintyVisualisationMode();
        if (uvMode == ImageInfo.UncertaintyVisualisationMode.Transparency_Blending) {
            for (int i = 0; i < colorPalette.length; i++) {
                int alpha = 255 - (256 * i) / colorPalette.length;
                colorPalette[i] = new Color(255, 255, 255, alpha);
            }
            return colorPalette;
        } else if (uvMode == ImageInfo.UncertaintyVisualisationMode.Monochromatic_Blending) {
            Color color = cpd.getLastPoint().getColor();
            for (int i = 0; i < colorPalette.length; i++) {
                int alpha = (256 * i) / colorPalette.length;
                colorPalette[i] = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            }
            return colorPalette;
        }

        final double minSample;
        final double maxSample;
        if (logScaled) {
            minSample = getSampleLog(cpd.getFirstPoint());
            maxSample = getSampleLog(cpd.getLastPoint());
        } else {
            minSample = getSample(cpd.getFirstPoint());
            maxSample = getSample(cpd.getLastPoint());
        }
        final double scalingFactor = 1 / (numColors - 1.0);
        int pointIndex = 0;
        final int maxPointIndex = cpd.getNumPoints() - 2;
        BorderSamplesAndColors boSaCo = getBorderSamplesAndColors(imageInfo, pointIndex, null);
        for (int i = 0; i < numColors - 1; i++) {
            final double w = i * scalingFactor;
            final double sample = minSample + w * (maxSample - minSample);
            if (sample >= boSaCo.sample2) {
                pointIndex++;
                pointIndex = Math.min(pointIndex, maxPointIndex);
                boSaCo = getBorderSamplesAndColors(imageInfo, pointIndex, boSaCo);
            }
            if (cpd.isDiscrete()) {
                colorPalette[i] = boSaCo.color1;
            } else {
                colorPalette[i] = computeColor(sample, boSaCo);
            }
        }
        colorPalette[numColors - 1] = boSaCo.color2;

        if (uvMode == ImageInfo.UncertaintyVisualisationMode.Polychromatic_Blending
                || uvMode == ImageInfo.UncertaintyVisualisationMode.Polychromatic_Overlay) {
            boolean blend = uvMode == ImageInfo.UncertaintyVisualisationMode.Polychromatic_Blending;
            int alpha = 127;
            for (int i = 0; i < colorPalette.length; i++) {
                Color color = colorPalette[i];
                if (blend) {
                    alpha = (256 * i) / colorPalette.length;
                }
                colorPalette[i] = new Color(color.getRed(),
                                            color.getGreen(),
                                            color.getBlue(),
                                            alpha);
            }
        }
        return colorPalette;
    }

    private static double getSample(ColorPaletteDef.Point point) {
        return point.getSample();
    }

    private static double getSampleLog(ColorPaletteDef.Point point) {
        return Stx.LOG10_SCALING.scale(getSample(point));
    }

    private static BorderSamplesAndColors getBorderSamplesAndColors(ImageInfo imageInfo, int pointIdx, BorderSamplesAndColors boSaCo) {
        if (boSaCo == null) {
            boSaCo = new BorderSamplesAndColors();
        }
        final boolean logScaled = imageInfo.isLogScaled();
        final ColorPaletteDef cpd = imageInfo.getColorPaletteDef();
        final ColorPaletteDef.Point p1 = cpd.getPointAt(pointIdx);
        final ColorPaletteDef.Point p2 = cpd.getPointAt(pointIdx + 1);
        if (logScaled) {
            boSaCo.sample1 = getSampleLog(p1);
            boSaCo.sample2 = getSampleLog(p2);
        } else {
            boSaCo.sample1 = getSample(p1);
            boSaCo.sample2 = getSample(p2);
        }
        boSaCo.color1 = p1.getColor();
        boSaCo.color2 = p2.getColor();
        return boSaCo;
    }

    private static class BorderSamplesAndColors {

        double sample1;
        double sample2;
        Color color1;
        Color color2;
    }

    private static Color computeColor(double sample, BorderSamplesAndColors boSaCo) {
        final double f = (sample - boSaCo.sample1) / (boSaCo.sample2 - boSaCo.sample1);
        final double r1 = boSaCo.color1.getRed();
        final double r2 = boSaCo.color2.getRed();
        final double g1 = boSaCo.color1.getGreen();
        final double g2 = boSaCo.color2.getGreen();
        final double b1 = boSaCo.color1.getBlue();
        final double b2 = boSaCo.color2.getBlue();
        final double a1 = boSaCo.color1.getAlpha();
        final double a2 = boSaCo.color2.getAlpha();
        final int red = (int) MathUtils.roundAndCrop(r1 + f * (r2 - r1), 0L, 255L);
        final int green = (int) MathUtils.roundAndCrop(g1 + f * (g2 - g1), 0L, 255L);
        final int blue = (int) MathUtils.roundAndCrop(b1 + f * (b2 - b1), 0L, 255L);
        final int alpha = (int) MathUtils.roundAndCrop(a1 + f * (a2 - a1), 0L, 255L);
        return new Color(red, green, blue, alpha);
    }

    // Initialization on demand holder idiom

    private static class Holder {

        private static final ImageManager instance = new ImageManager();

        private Holder() {
        }
    }
}

