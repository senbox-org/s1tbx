package org.esa.beam.jai;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.IntMap;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.math.Histogram;

import javax.media.jai.*;
import javax.media.jai.operator.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;


public class ImageManager {
    private static final ColorPaletteDef.Point OTHER_POINT = new ColorPaletteDef.Point(Double.NaN, Color.BLACK, "Other");

    private final static ImageManager INSTANCE = new ImageManager();
    private final Map<Object, MultiLevelSource> maskImageMap = new WeakHashMap<Object, MultiLevelSource>(101);

    public static ImageManager getInstance() {
        return INSTANCE;
    }

    public synchronized void dispose() {
        for (MultiLevelSource multiLevelSource : maskImageMap.values()) {
            multiLevelSource.reset();
        }
        maskImageMap.clear();
    }

    public MultiLevelModel createMultiLevelModel(ProductNode productNode) {
        final Scene scene = SceneFactory.createScene(productNode);
        if (scene == null) {
            return null;
        }
        final int w = scene.getRasterWidth();
        final int h = scene.getRasterHeight();
        // TODO - BLOCKER: Use scene.getGeoCoding().getMapInfo() to construct i2mTransform. SMOS will not work otherwise.  (nf, 19.09.2008)
        final AffineTransform i2mTransform = new AffineTransform();
        return new DefaultMultiLevelModel(i2mTransform, w, h);
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
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataType, width, height);
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
        int destWidth = (int) Math.floor(sourceWidth / level.getScale());
        int destHeight = (int) Math.floor(sourceHeight / level.getScale());
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataBufferType,
                                                                           destWidth,
                                                                           destHeight);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        tileSize = tileSize != null ? tileSize : JAIUtils.computePreferredTileSize(destWidth, destHeight, 1);
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

    private static class MaskKey {
        final WeakReference<Product> product;
        final String expression;

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

    // TODO - BLOCKER: ImageInfo.rgbChannelDef is not used for 3-band images! (nf, 19.09.2008)
    // Solution: Split into following methods, use ProductUtils code as reference.
    //
    //    public PlanarImage createColored1BandImage(RasterDataNode raster, ImageInfo imageInfo,
    //                                               int level, int levelCount);
    //
    //    public PlanarImage createColored3BandImage(RasterDataNode[] rasters, ImageInfo imageInfo,
    //                                               int level, int levelCount);
    //
    public PlanarImage createColoredBandImage(final RasterDataNode[] rasterDataNodes,
                                              final int level,
                                              int levelCount) {
        Assert.notNull(rasterDataNodes,
                       "rasterDataNodes");
        Assert.state(rasterDataNodes.length == 1
                || rasterDataNodes.length == 3
                || rasterDataNodes.length == 4,
                     "invalid number of bands");

        PlanarImage image;

        PlanarImage[] bandImages = getBandImages(rasterDataNodes, level);
        PlanarImage[] validMaskImages = getValidMaskImages(rasterDataNodes, level);
        prepareImageInfos(rasterDataNodes, bandImages, validMaskImages, levelCount, ProgressMonitor.NULL);

        PlanarImage[] sourceImages = new PlanarImage[rasterDataNodes.length];
        for (int i = 0; i < rasterDataNodes.length; i++) {
            final RasterDataNode raster = rasterDataNodes[i];
            PlanarImage planarImage = bandImages[i];
            ImageInfo imageInfo = raster.getImageInfo();
            Assert.state(imageInfo != null, "imageInfo != null");
            final double minSample = imageInfo.getColorPaletteDef().getMinDisplaySample();
            final double maxSample = imageInfo.getColorPaletteDef().getMaxDisplaySample();
            Assert.notNull(imageInfo, "imageInfo");

            final IndexCoding indexCoding = (raster instanceof Band) ? ((Band) raster).getIndexCoding() : null;
            if (indexCoding != null) {
                final IntMap sampleColorIndexMap = new IntMap((int) minSample - 1, 4098);
                final ColorPaletteDef.Point[] points = imageInfo.getColorPaletteDef().getPoints();
                for (int colorIndex = 0; colorIndex < points.length; colorIndex++) {
                    sampleColorIndexMap.putValue((int) points[colorIndex].getSample(), colorIndex);
                }

                planarImage = JAIUtils.createIndexedImage(planarImage, sampleColorIndexMap,
                                                          raster.getImageInfo().getColorPaletteDef().getNumPoints());
            } else {
                final double newMin = raster.scaleInverse(minSample);
                final double newMax = raster.scaleInverse(maxSample);
                planarImage = JAIUtils.createRescaleOp(planarImage,
                                                       255.0 / (newMax - newMin),
                                                       255.0 * newMin / (newMin - newMax));
                planarImage = JAIUtils.createFormatOp(planarImage,
                                                      DataBuffer.TYPE_BYTE);
            }
            sourceImages[i] = planarImage;
        }
        if (rasterDataNodes.length == 1) {
            image = performIndexToRgbConversion1Band(rasterDataNodes[0], sourceImages[0], validMaskImages[0]);
        } else {
            image = performIndexToRgbConversion3Bands(rasterDataNodes, sourceImages, validMaskImages);
        }

        return image;
    }

    public PlanarImage getBandImage(RasterDataNode rasterDataNode, int level) {
        RenderedImage sourceImage = rasterDataNode.getSourceImage();
        return getLevelImage(sourceImage, level);
    }
    
    public PlanarImage[] getBandImages(RasterDataNode[] rasterDataNodes, int level) {
        PlanarImage[] planarImages = new PlanarImage[rasterDataNodes.length];
        for (int i = 0; i < rasterDataNodes.length; i++) {
            RasterDataNode raster = rasterDataNodes[i];
            planarImages[i] = getBandImage(raster, level);
        }
        return planarImages;
    }
    
    public PlanarImage getValidMaskImage(final RasterDataNode rasterDataNode, int level) {
        if (rasterDataNode.isValidMaskUsed()) {
            return getLevelImage(rasterDataNode.getValidMaskImage(), level);
        }
        return null;
    }

    public PlanarImage[] getValidMaskImages(RasterDataNode[] rasterDataNodes, int level) {
        final PlanarImage[] images = new PlanarImage[rasterDataNodes.length];
        for (int i = 0; i < rasterDataNodes.length; i++) {
            images[i] = getValidMaskImage(rasterDataNodes[i], level);
        }
        return images;
    }

    public PlanarImage getGeophysicalImage(RasterDataNode rasterDataNode, int level) {
        RenderedImage levelZeroImage = rasterDataNode.getGeophysicalImage();
        return getLevelImage(levelZeroImage, level);
    }
    
    public MultiLevelSource getMultiLevelSource(RenderedImage levelZeroImage) {
        MultiLevelSource multiLevelSource;
        if (levelZeroImage instanceof MultiLevelSource) {
            multiLevelSource = (MultiLevelSource) levelZeroImage;
        } else {
            // TODO - New image instance is created here. This will happen e.g. for all bands created by GPF operators. (nf, 19.09.2008)
            final int levelCount = DefaultMultiLevelModel.getLevelCount(levelZeroImage.getWidth(), levelZeroImage.getHeight());
            multiLevelSource = new DefaultMultiLevelSource(levelZeroImage, 
                                                           levelCount, 
                                                           Interpolation.getInstance(Interpolation.INTERP_NEAREST));
            System.out.println("IMAGING 4.5: " +
            		"Warning: Created an (inefficient) instance of DefaultMultiLevelSource. " +
            		"Source image is a " + levelZeroImage.getClass());
        }
        return multiLevelSource;
    }

    private PlanarImage getLevelImage(RenderedImage levelZeroImage, int level) {
        final MultiLevelSource multiLevelSource = getMultiLevelSource(levelZeroImage);
        RenderedImage image = multiLevelSource.getImage(level);
        return PlanarImage.wrapRenderedImage(image);
    }

    public SingleBandedOpImage getMaskImage(final Product product, final String expression, int level) {
        final Object key = new MaskKey(product, expression);
        synchronized (maskImageMap) {
            MultiLevelSource mrMulti = maskImageMap.get(key);
            if (mrMulti == null) {
                mrMulti = new AbstractMultiLevelSource(createMultiLevelModel(product)) {

                    @Override
                    public RenderedImage createImage(int level) {
                        return VirtualBandOpImage.createMaskOpImage(product, expression, ResolutionLevel.create(getModel(), level));
                    }
                };
                maskImageMap.put(key, mrMulti);
            }
            // Note: cast is ok, because interface of MultiLevelSource requires to return same type
            return (SingleBandedOpImage) mrMulti.getImage(level);
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


    public void prepareImageInfos(RasterDataNode[] rasterDataNodes, int levelCount, ProgressMonitor pm) {
        PlanarImage[] bandImages = getBandImages(rasterDataNodes, 0);
        PlanarImage[] validMaskImages = getValidMaskImages(rasterDataNodes, 0);
        prepareImageInfos(rasterDataNodes, bandImages, validMaskImages, levelCount, pm);
    }

    private void prepareImageInfos(RasterDataNode[] rasterDataNodes,
                                   PlanarImage[] bandImages,
                                   PlanarImage[] validMaskImages,
                                   int levelCount,
                                   ProgressMonitor pm) {

        int numTaskSteps = 0;
        for (RasterDataNode raster : rasterDataNodes) {
            numTaskSteps += raster.getImageInfo() == null ? 1 : 0;
        }

        pm.beginTask("Computing image statistics", numTaskSteps);
        try {
            for (int i = 0; i < rasterDataNodes.length; i++) {
                RasterDataNode raster = rasterDataNodes[i];

                ImageInfo imageInfo = raster.getImageInfo();
                if (imageInfo == null) {
                    raster.getImageInfo(SubProgressMonitor.create(pm, 1));
                }
            }
        } finally {
            pm.done();
        }
    }
    
    public int getStatisticsLevel(RasterDataNode raster, int levelCount) {
        final long imageSize = (long) raster.getSceneRasterWidth() * raster.getSceneRasterHeight();
        final int statisticsLevel;
        if (imageSize <= DefaultMultiLevelModel.MAX_PIXEL_COUNT) {
            statisticsLevel = 0;
        } else {
            statisticsLevel = levelCount - 1;
        }
        return statisticsLevel;
    }
    private static PlanarImage performIndexToRgbConversion3Bands(RasterDataNode[] rasterDataNodes,
                                                           PlanarImage[] sourceImages,
                                                           PlanarImage[] maskOpImages) {
        PlanarImage alpha = null;
        for (PlanarImage maskOpImage : maskOpImages) {
            if (maskOpImage != null) {
                if (alpha != null) {
                    alpha = MaxDescriptor.create(alpha, maskOpImage, null);
                } else {
                    alpha = maskOpImage;
                }
            }
        }
        RenderedOp image = BandMergeDescriptor.create(sourceImages[0], sourceImages[1], null);
        image = BandMergeDescriptor.create(image, sourceImages[2], null);
        if (alpha != null) {
            image = BandMergeDescriptor.create(image, alpha, null);
        }
        return image;
    }
    
    private static PlanarImage performIndexToRgbConversion1Band(RasterDataNode rasterDataNode,
                                                          PlanarImage sourceImage,
                                                          PlanarImage maskOpImage) {
        Color[] palette;
        ColorPaletteDef colorPaletteDef = rasterDataNode.getImageInfo().getColorPaletteDef();
        if ((rasterDataNode instanceof Band) && ((Band) rasterDataNode).getIndexCoding() != null) {
            Color[] origPalette = colorPaletteDef.getColors();
            palette = Arrays.copyOf(origPalette, origPalette.length + 1);
            palette[palette.length-1] = rasterDataNode.getImageInfo().getNoDataColor();
        } else {
            palette = colorPaletteDef.createColorPalette(rasterDataNode);
        }
        final byte[][] lutData = new byte[3][palette.length];
        for (int i = 0; i < palette.length; i++) {
            lutData[0][i] = (byte) palette[i].getRed();
            lutData[1][i] = (byte) palette[i].getGreen();
            lutData[2][i] = (byte) palette[i].getBlue();
        }
        RenderedOp image = JAIUtils.createLookupOp(sourceImage, lutData);
        if (maskOpImage != null) {
            image = BandMergeDescriptor.create(image, maskOpImage, null);
        }
        return image;
    }

    public PlanarImage createColoredMaskImage(Product product, String expression, Color color, boolean invertMask, int level) {
        PlanarImage image = getMaskImage(product, expression, level);
        return createColoredMaskImage(color, image, invertMask);
    }

    public static PlanarImage createColoredMaskImage(Color color, PlanarImage alphaImage, boolean invertAlpha) {
        return createColoredMaskImage(color, invertAlpha ? InvertDescriptor.create(alphaImage, null) : alphaImage);
    }

    public static PlanarImage createColoredMaskImage(Color color, PlanarImage alphaImage) {
        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setTileWidth(alphaImage.getTileWidth());
        imageLayout.setTileHeight(alphaImage.getTileHeight());
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        final RenderedOp colorImage = ConstantDescriptor.create((float) alphaImage.getWidth(), (float) alphaImage.getHeight(), new Byte[]{
                (byte) color.getRed(),
                (byte) color.getGreen(),
                (byte) color.getBlue(),
        }, hints);
        return BandMergeDescriptor.create(colorImage, alphaImage, hints);
    }

    /**
     * Creates a colored ROI image for the given band.
     *
     * @param rasterDataNode the band
     * @param color          the color
     * @param level          the level
     * @return the image, or null if the band has no valid ROI definition
     */
    public PlanarImage createColoredRoiImage(RasterDataNode rasterDataNode, Color color, int level) {
        final PlanarImage roi = createRoiMaskImage(rasterDataNode, level);
        if (roi == null) {
            return null;
        }
        return createColoredMaskImage(color, roi, false);
    }

    /**
     * Creates a ROI for the given band.
     *
     * @param rasterDataNode the band
     * @param level          the level
     * @return the ROI, or null if the band has no valid ROI definition
     */
    public PlanarImage createRoiMaskImage(final RasterDataNode rasterDataNode, int level) {
        final ROIDefinition roiDefinition = rasterDataNode.getROIDefinition();
        if (roiDefinition == null) {
            return null;
        }

        ArrayList<PlanarImage> rois = new ArrayList<PlanarImage>(4);

        // Step 1:  insert ROI pixels determined by bitmask expression
        String bitmaskExpr = roiDefinition.getBitmaskExpr();
        if (!StringUtils.isNullOrEmpty(bitmaskExpr) && roiDefinition.isBitmaskEnabled()) {
            rois.add(getMaskImage(rasterDataNode.getProduct(), bitmaskExpr, level));
        }

        // Step 2:  insert ROI pixels within value range
        if (roiDefinition.isValueRangeEnabled()) {
            String rangeExpr = rasterDataNode.getName() + " >= " + roiDefinition.getValueRangeMin() + " && "
                    + rasterDataNode.getName() + " <= " + roiDefinition.getValueRangeMax();
            rois.add(getMaskImage(rasterDataNode.getProduct(), rangeExpr, level));
        }

        // Step 3:  insert ROI pixels for pins
        final MultiLevelModel multiLevelModel = createMultiLevelModel(rasterDataNode);
        if (roiDefinition.isPinUseEnabled() && rasterDataNode.getProduct().getPinGroup().getNodeCount() > 0) {

            final Object key = new MaskKey(rasterDataNode.getProduct(), rasterDataNode.getName() + "_RoiPlacemarks");
            MultiLevelSource placemarkMaskMLS;
            synchronized (maskImageMap) {
                placemarkMaskMLS = maskImageMap.get(key);
                if (placemarkMaskMLS == null) {
                    placemarkMaskMLS = new AbstractMultiLevelSource(multiLevelModel) {

                        @Override
                        public RenderedImage createImage(int level) {
                            return new PlacemarkMaskOpImage(rasterDataNode.getProduct(),
                                                            PinDescriptor.INSTANCE, 3,
                                                            rasterDataNode.getSceneRasterWidth(),
                                                            rasterDataNode.getSceneRasterHeight(),
                                                            ResolutionLevel.create(getModel(), level));
                        }
                    };
                    maskImageMap.put(key, placemarkMaskMLS);
                }
            }
            rois.add((PlanarImage) placemarkMaskMLS.getImage(level));
        }

        // Step 4:  insert ROI pixels within shape
        Figure roiShapeFigure = roiDefinition.getShapeFigure();
        if (roiDefinition.isShapeEnabled() && roiShapeFigure != null) {

            final Object key = new MaskKey(rasterDataNode.getProduct(), rasterDataNode.getName() + "_RoiShapes");
            MultiLevelSource shapeMaskMLS;
            synchronized (maskImageMap) {
                shapeMaskMLS = maskImageMap.get(key);
                if (shapeMaskMLS == null) {
                    final Shape roiShape = roiShapeFigure.getShape();
                    shapeMaskMLS = new AbstractMultiLevelSource(multiLevelModel) {

                        @Override
                        public RenderedImage createImage(int level) {
                            return new ShapeMaskOpImage(roiShape,
                                                        rasterDataNode.getSceneRasterWidth(),
                                                        rasterDataNode.getSceneRasterHeight(),
                                                        ResolutionLevel.create(getModel(), level));
                        }
                    };
                    maskImageMap.put(key, shapeMaskMLS);
                }
            }
            rois.add((PlanarImage) shapeMaskMLS.getImage(level));
        }

        if (rois.size() == 0) {
            // todo - null is returned whenever a shape is converted into a ROI for any but the first time
            // todo - may be this problem is due to concurrency issues
            return null;
        }

        PlanarImage roi = rois.get(0);

        // Step 5: combine ROIs
        for (int i = 1; i < rois.size(); i++) {
            PlanarImage roi2 = rois.get(i);
            if (roiDefinition.isOrCombined()) {
                roi = MaxDescriptor.create(roi, roi2, null);
            } else {
                roi = MinDescriptor.create(roi, roi2, null);
            }
        }

        // Step 6:  invert ROI pixels
        if (roiDefinition.isInverted()) {
            roi = InvertDescriptor.create(roi, null);
        }

        return roi;
    }
}
