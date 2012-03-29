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

package org.esa.beam.jai;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.IntMap;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.math.MathUtils;
import org.geotools.referencing.crs.DefaultImageCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.datum.DefaultImageDatum;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ImageCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import javax.media.jai.*;
import javax.media.jai.operator.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * This class provides most of the new imaging features introduced in BEAM 4.5.
 * <p><i>WARNING:</i> Although {@code ImageManager} is intended to belong to the public BEAM API you should use it
 * with care, since it is still under development and may change slightly in forthcoming versions.</p>
 */
public class ImageManager {

    /**
     * The default BEAM image coordinate reference system.
     */
    public static final ImageCRS DEFAULT_IMAGE_CRS = new DefaultImageCRS("BEAM",
                                                                         new DefaultImageDatum("BEAM", PixelInCell.CELL_CORNER),
                                                                         DefaultCartesianCS.DISPLAY);

    private static final boolean CACHE_INTERMEDIATE_TILES = Boolean.getBoolean(
            "beam.imageManager.enableIntermediateTileCaching");

    private final Map<MaskKey, MultiLevelImage> maskImageMap = new HashMap<MaskKey, MultiLevelImage>(101);
    private final ProductNodeListener rasterDataChangeListener;

    public static ImageManager getInstance() {
        return Holder.instance;
    }

    public ImageManager() {
        this.rasterDataChangeListener = new RasterDataChangeListener();
    }

    public synchronized void dispose() {
        for (MultiLevelSource multiLevelSource : maskImageMap.values()) {
            multiLevelSource.reset();
        }
        maskImageMap.clear();
    }

    public static MultiLevelModel getMultiLevelModel(RasterDataNode rasterDataNode) {
        if (rasterDataNode.isSourceImageSet()) {
            return rasterDataNode.getSourceImage().getModel();
        }
        return createMultiLevelModel(rasterDataNode);
    }

    public static AffineTransform getImageToModelTransform(GeoCoding geoCoding) {
        if (geoCoding == null) {
            return new AffineTransform();
        }
        final MathTransform image2Map = geoCoding.getImageToMapTransform();
        if (image2Map instanceof AffineTransform) {
            return new AffineTransform((AffineTransform) image2Map);
        }
        return new AffineTransform();
    }

    /**
     * Gets the coordinate reference system used for the model space. The model space is coordinate system
     * that is used to render images for display.
     *
     * @param geoCoding A geo-coding, may be {@code null}.
     * @return The coordinate reference system used for the model space. If {@code geoCoding} is {@code null},
     *         it will be a default image coordinate reference system (an instance of {@code org.opengis.referencing.crs.ImageCRS}).
     */
    public static CoordinateReferenceSystem getModelCrs(GeoCoding geoCoding) {
        if (geoCoding != null) {
            final MathTransform image2Map = geoCoding.getImageToMapTransform();
            if (image2Map instanceof AffineTransform) {
                return geoCoding.getMapCRS();
            }
            return geoCoding.getImageCRS();
        } else {
            return DEFAULT_IMAGE_CRS;
        }
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
        int width = rasterDataNode.getSceneRasterWidth();
        int height = rasterDataNode.getSceneRasterHeight();
        Dimension tileSize = getPreferredTileSize(rasterDataNode.getProduct());
        return createSingleBandedImageLayout(dataBufferType, width, height, tileSize.width, tileSize.height);
    }

    public static ImageLayout createSingleBandedImageLayout(int dataType,
                                                            int width,
                                                            int height,
                                                            int tileWidth,
                                                            int tileHeight) {
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataType, tileWidth, tileHeight);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        return createImageLayout(width, height, tileWidth, tileHeight, sampleModel, colorModel);
    }

    public static ImageLayout createSingleBandedImageLayout(int dataBufferType,
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
        final Dimension destDimension = AbstractMultiLevelSource.getImageDimension(sourceWidth,
                                                                                   sourceHeight,
                                                                                   level.getScale());
        final int destWidth = destDimension.width;
        final int destHeight = destDimension.height;
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

        return createImageLayout(destWidth, destHeight, tileSize.width, tileSize.height, sampleModel, colorModel);
    }

    private static ImageLayout createImageLayout(int width,
                                                 int height,
                                                 int tileWidth,
                                                 int tileHeight,
                                                 SampleModel sampleModel,
                                                 ColorModel colorModel) {
        return new ImageLayout(0, 0,
                               width,
                               height,
                               0, 0,
                               tileWidth,
                               tileHeight,
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
                     "invalid number of bands");

        prepareImageInfos(rasterDataNodes, ProgressMonitor.NULL);
        if (rasterDataNodes.length == 1) {
            return createColored1BandImage(rasterDataNodes[0], imageInfo, level);
        } else {
            return createColored3BandImage(rasterDataNodes, imageInfo, level);
        }
    }

    public static MultiLevelModel createMultiLevelModel(ProductNode productNode) {
        final Scene scene = SceneFactory.createScene(productNode);
        if (scene == null) {
            return null;
        }
        final int w = scene.getRasterWidth();
        final int h = scene.getRasterHeight();

        final AffineTransform i2mTransform = getImageToModelTransform(scene.getGeoCoding());
        return new DefaultMultiLevelModel(i2mTransform, w, h);
    }

    private RenderedImage createColored1BandImage(RasterDataNode raster, ImageInfo imageInfo, int level) {
        Assert.notNull(raster, "raster");
        Assert.notNull(imageInfo, "imageInfo");
        RenderedImage sourceImage = getSourceImage(raster, level);
        RenderedImage validMaskImage = getValidMaskImage(raster, level);
        PlanarImage image = createByteIndexedImage(raster, sourceImage, imageInfo);
        image = createMatchCdfImage(image, imageInfo.getHistogramMatching(), new Stx[]{raster.getStx()});
        image = createLookupRgbImage(raster, image, validMaskImage, imageInfo);
        return image;
    }

    private PlanarImage createColored3BandImage(RasterDataNode[] rasters, ImageInfo rgbImageInfo, int level) {
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
        // todo - correctly handle no-data color (nf, 10.10.2008)
        return createMergeRgbaOp(images, validMaskImages, rgbImageInfo.getHistogramMatching(), stxs);
    }

    private static PlanarImage createByteIndexedImage(RasterDataNode raster,
                                                      RenderedImage sourceImage,
                                                      ImageInfo imageInfo) {
        ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        final double minSample = colorPaletteDef.getMinDisplaySample();
        final double maxSample = colorPaletteDef.getMaxDisplaySample();

        final IndexCoding indexCoding = (raster instanceof Band) ? ((Band) raster).getIndexCoding() : null;
        if (indexCoding != null) {
            final IntMap sampleColorIndexMap = new IntMap((int) minSample - 1, 4098);
            final ColorPaletteDef.Point[] points = colorPaletteDef.getPoints();
            for (int colorIndex = 0; colorIndex < points.length; colorIndex++) {
                sampleColorIndexMap.putValue((int) points[colorIndex].getSample(), colorIndex);
            }
            final int undefinedIndex = colorPaletteDef.getNumPoints();
            return createIndexedImage(sourceImage, sampleColorIndexMap, undefinedIndex);
        } else {
            return createByteIndexedImage(raster, sourceImage, minSample, maxSample, 1.0);
        }
    }

    private static PlanarImage createByteIndexedImage(RasterDataNode raster,
                                                      RenderedImage sourceImage,
                                                      double minSample,
                                                      double maxSample,
                                                      double gamma) {
        double newMin = raster.scaleInverse(minSample);
        double newMax = raster.scaleInverse(maxSample);
        PlanarImage image = createRescaleOp(sourceImage,
                                            255.0 / (newMax - newMin),
                                            255.0 * newMin / (newMin - newMax));
        // todo - make sure this is not needed, e.g. does "format" auto-clamp?? (nf, 10.2008)
        // image = createClampOp(image, 0, 255);
        image = createByteFormatOp(image);
        if (gamma != 0.0 && gamma != 1.0) {
            byte[] gammaCurve = MathUtils.createGammaCurve(gamma, new byte[256]);
            LookupTableJAI lookupTable = new LookupTableJAI(gammaCurve);
            image = LookupDescriptor.create(image, lookupTable, createDefaultRenderingHints(image, null));
        }
        return image;
    }

    private static RenderingHints createDefaultRenderingHints(RenderedImage sourceImage, ImageLayout targetLayout) {
        Map<RenderingHints.Key, Object> map = new HashMap<RenderingHints.Key, Object>(7);
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

    private static PlanarImage createLookupRgbImage(RasterDataNode rasterDataNode,
                                                    RenderedImage sourceImage,
                                                    RenderedImage maskImage,
                                                    ImageInfo imageInfo) {
        Color[] palette;
        ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        if (rasterDataNode instanceof Band && ((Band) rasterDataNode).getIndexCoding() != null) {
            Color[] origPalette = colorPaletteDef.getColors();
            palette = Arrays.copyOf(origPalette, origPalette.length + 1);
            palette[palette.length - 1] = imageInfo.getNoDataColor();
        } else {
            palette = colorPaletteDef.createColorPalette(rasterDataNode);
        }

        final byte[][] lutData = new byte[3][palette.length];
        for (int i = 0; i < palette.length; i++) {
            lutData[0][i] = (byte) palette[i].getRed();
            lutData[1][i] = (byte) palette[i].getGreen();
            lutData[2][i] = (byte) palette[i].getBlue();
        }
        PlanarImage image = createLookupOp(sourceImage, lutData);
        if (maskImage != null) {
            final Color noDataColor = imageInfo.getNoDataColor();
            final Byte[] noDataRGB = new Byte[]{
                    (byte) noDataColor.getRed(),
                    (byte) noDataColor.getGreen(),
                    (byte) noDataColor.getBlue()
            };
            final RenderedOp noDataColorImage = ConstantDescriptor.create((float) image.getWidth(),
                                                                          (float) image.getHeight(),
                                                                          noDataRGB,
                                                                          createDefaultRenderingHints(sourceImage, null));
            byte noDataAlpha = (byte) noDataColor.getAlpha();
            final RenderedOp noDataAlphaImage = ConstantDescriptor.create((float) image.getWidth(),
                                                                          (float) image.getHeight(),
                                                                          new Byte[]{noDataAlpha},
                                                                          createDefaultRenderingHints(sourceImage, null));

            image = CompositeDescriptor.create(image, noDataColorImage,
                                               maskImage, noDataAlphaImage, false,
                                               CompositeDescriptor.DESTINATION_ALPHA_LAST,
                                               createDefaultRenderingHints(sourceImage, null));
        }

        return image;
    }

    private static PlanarImage createMatchCdfImage(PlanarImage sourceImage,
                                                   ImageInfo.HistogramMatching histogramMatching, Stx[] stxs) {
        final boolean doEqualize = ImageInfo.HistogramMatching.Equalize == histogramMatching;
        final boolean doNormalize = ImageInfo.HistogramMatching.Normalize == histogramMatching;
        if (doEqualize || doNormalize) {
            if (doEqualize) {
                sourceImage = createMatchCdfEqualizeImage(sourceImage, stxs);
            } else {
                sourceImage = createMatchCdfNormalizeImage(sourceImage, stxs);
            }
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

    @Deprecated
    public MultiLevelImage createValidMaskMultiLevelImage(final RasterDataNode rasterDataNode) {
        final MultiLevelModel model = ImageManager.getMultiLevelModel(rasterDataNode);
        final MultiLevelSource mls = new AbstractMultiLevelSource(model) {

            @Override
            public RenderedImage createImage(int level) {
                return VirtualBandOpImage.createMask(rasterDataNode,
                                                     ResolutionLevel.create(getModel(), level));
            }
        };
        return new DefaultMultiLevelImage(mls);
    }

    public RenderedImage getMaskImage(final Product product, final String expression, int level) {
        MultiLevelImage mli = getMaskImage(expression, product);
        return mli.getImage(level);
    }

    public MultiLevelImage getMaskImage(final String expression, final Product product) {
        synchronized (maskImageMap) {
            final MaskKey key = new MaskKey(product, expression);
            MultiLevelImage mli = maskImageMap.get(key);
            if (mli == null) {
                MultiLevelSource mls = new AbstractMultiLevelSource(createMultiLevelModel(product)) {

                    @Override
                    public RenderedImage createImage(int level) {
                        return VirtualBandOpImage.createMask(expression,
                                                             product,
                                                             ResolutionLevel.create(getModel(), level));
                    }
                };
                mli = new DefaultMultiLevelImage(mls);
                product.addProductNodeListener(rasterDataChangeListener);
                maskImageMap.put(key, mli);
            }
            return mli;
        }
    }

    public ImageInfo getImageInfo(RasterDataNode[] rasters) {
        Assert.notNull(rasters, "rasters");
        Assert.argument(rasters.length == 1 || rasters.length == 3, "rasters.length == 1 || rasters.length == 3");
        if (rasters.length == 1) {
            Assert.state(rasters[0].getImageInfo() != null, "rasters[0].getImageInfo()");
            return rasters[0].getImageInfo();
        } else {
            final RGBChannelDef rgbChannelDef = new RGBChannelDef();
            for (int i = 0; i < rasters.length; i++) {
                RasterDataNode raster = rasters[i];
                Assert.state(rasters[i].getImageInfo() != null, "rasters[i].getImageInfo()");
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
        final long imageSize = (long) raster.getSceneRasterWidth() * raster.getSceneRasterHeight();
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
        RenderedImage image = getMaskImage(product, expression, level);
        return createColoredMaskImage(color, image, invertMask);
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
                        }, hints);
        return BandMergeDescriptor.create(colorImage, alphaImage, hints);
    }

    public static RenderedImage createFormatOp(RenderedImage image, int dataType) {
        if (image.getSampleModel().getDataType() == dataType) {
            return PlanarImage.wrapRenderedImage(image);
        }
        return FormatDescriptor.create(image,
                                       dataType,
                                       createDefaultRenderingHints(image, null));
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

    private static class MaskKey {

        private final WeakReference<Product> product;
        private final String expression;

        private MaskKey(Product product, String expression) {
            Assert.notNull(product, "product");
            Assert.notNull(expression, "expression");
            this.product = new WeakReference<Product>(product);
            this.expression = expression;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (getClass() != o.getClass()) {
                return false;
            }
            MaskKey key = (MaskKey) o;
            return product.get() == key.product.get() && expression.equals(key.expression);

        }

        @Override
        public int hashCode() {
            int result;
            result = product.get().hashCode();
            result = 31 * result + expression.hashCode();
            return result;
        }
    }

    private class RasterDataChangeListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            super.nodeDataChanged(event);
            final ProductNode node = event.getSourceNode();
            synchronized (maskImageMap) {
                final Set<MaskKey> keySet = maskImageMap.keySet();
                for (MaskKey maskKey : keySet) {
                    if (maskKey.product.get() == node.getProduct() && maskKey.expression.contains(node.getName())) {
                        maskImageMap.get(maskKey).reset();
                    }
                }
            }
        }
    }

    // Initialization on demand holder idiom

    private static class Holder {

        private static final ImageManager instance = new ImageManager();

        private Holder() {
        }
    }
}

