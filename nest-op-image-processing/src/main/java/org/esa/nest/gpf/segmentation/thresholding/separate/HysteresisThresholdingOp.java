/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.segmentation.thresholding.separate;

import com.bc.ceres.core.ProgressMonitor;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.gpf.OperatorUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * This plug-in takes as parameters a grayscale image and two thresholds (low
 * and high), and returns the hysteresis thresholded image
 *
 * @author Emanuela Boros
 */
@OperatorMetadata(alias = "HysteresisThresholding",
        category = "Image Processing",
        authors = "Emanuela Boros",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "HysteresisThresholding")
public class HysteresisThresholdingOp extends Operator {

    public static float[] probabilityHistogram;
    final static int MAX_VALUE = 256;
    final static int MIN_VALUE = 0;
    public static int N;
    @SourceProduct(alias = "source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;
    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
    rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;
    @Parameter(description = "HighThreshold", defaultValue = "100", label = "HighThreshold")
    private float highThreshold = 100f;
    @Parameter(description = "LowThreshold", defaultValue = "10", label = "LowThreshold")
    private float lowThreshold = 10f;
    private final Map<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();
    private int sourceImageWidth;
    private int sourceImageHeight;
    private boolean processed = false;
    private int halfSizeX;
    private int halfSizeY;
    private int filterSizeX = 3;
    private int filterSizeY = 3;
    private static ImagePlus fullImagePlus;
    private static ByteProcessor fullByteProcessor;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type
     * {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct}
     * annotation or by calling {@link #setTargetProduct} method.</p> <p>The
     * framework calls this method after it has created this operator. Any
     * client code that must be performed before computation of tile data should
     * be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs
     * during operator initialization.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            halfSizeX = filterSizeX / 2;
            halfSizeY = filterSizeY / 2;

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     *
     * @throws Exception The exception.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        OperatorUtils.addSelectedBands(
                sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, true, true);
    }

    /**
     * Called by the framework in order to compute a tile for the given target
     * band. <p>The default implementation throws a runtime exception with the
     * message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be
     * computed.
     * @param pm A progress monitor which should be used to determine
     * computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs
     * during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;

            final Rectangle sourceTileRectangle = getSourceTileRectangle(x0, y0, w, h);
            Tile sourceRaster;
            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
            Band sourceBand = sourceProduct.getBand(srcBandNames[0]);
            sourceRaster = getSourceTile(sourceBand, sourceTileRectangle);
            if (sourceRaster == null) {
                throw new OperatorException("Cannot get source tile");
            }

            computeHysteresisThesholding(sourceBand, sourceRaster, targetTile, x0, y0, w, h, pm);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Apply HysteresisThesholding
     *
     * @param sourceRaster The source tile for the band.
     * @param targetTile The current tile associated with the target band to be
     * computed.
     * @param x0 X coordinate for the upper-left point of the
     * target_Tile_Rectangle.
     * @param y0 Y coordinate for the upper-left point of the
     * target_Tile_Rectangle.
     * @param w Width for the target_Tile_Rectangle.
     * @param h Hight for the target_Tile_Rectangle.
     * @param pm A progress monitor which should be used to determine
     * computation cancellation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs
     * during computation of the filtered value.
     */
    private synchronized void computeHysteresisThesholding(final Band sourceBand, final Tile sourceRaster,
            final Tile targetTile, final int x0, final int y0, final int w, final int h,
            final ProgressMonitor pm) {

        if (!processed) {
            final RenderedImage fullRenderedImage = sourceBand.getSourceImage().getImage(0);
            final BufferedImage fullBufferedImage = new BufferedImage(sourceBand.getSceneRasterWidth(),
                    sourceBand.getSceneRasterHeight(),
                    BufferedImage.TYPE_USHORT_GRAY);
            fullBufferedImage.setData(fullRenderedImage.getData());

            fullImagePlus = new ImagePlus(sourceBand.getDisplayName(), fullBufferedImage);

//            final ImageProcessor fullImageProcessor = fullImagePlus.getProcessor();

            ImageStack stack = fullImagePlus.getStack();
            ImageStack res_trin = new ImageStack(stack.getWidth(), stack.getHeight());
            ImageStack res_hyst = new ImageStack(stack.getWidth(), stack.getHeight());

            ImageProcessor tmp1 = null;
            ImageProcessor tmp2 = null;

            for (int s = 1; s <= stack.getSize(); s++) {
//                tmp1 = ImageEdge.trin(stack.getProcessor(s), highThreshold, lowThreshold);
                tmp1 = (ByteProcessor) trinarise((ByteProcessor) stack.getProcessor(s).convertToByte(true),
                        highThreshold, lowThreshold);
                res_trin.addSlice("", tmp1);
                tmp2 = (ByteProcessor) hysteresisThresholding((ByteProcessor) tmp1.convertToByte(true));
                res_hyst.addSlice("", tmp2);

            }
            fullByteProcessor = (ByteProcessor) new ImagePlus("Hysteresis", res_hyst).getProcessor().convertToByte(true);
//            fullByteProcessor = (ByteProcessor) fullImageProcessor.convertToByte(true);
//            fullByteProcessor = (ByteProcessor) trinarise(fullByteProcessor, highThreshold, lowThreshold);
//            fullByteProcessor = (ByteProcessor) hysteresisThresholding(fullByteProcessor);

            processed = true;
        }

        final Rectangle srcTileRectangle = sourceRaster.getRectangle();

        ImageProcessor aPartProcessor = fullByteProcessor.duplicate();

        aPartProcessor.setRoi(srcTileRectangle);

        ImageProcessor roiImageProcessor = aPartProcessor.crop();

        final ProductData trgData = targetTile.getDataBuffer();
        final ProductData sourceData = ProductData.createInstance((byte[]) roiImageProcessor.getPixels());

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            for (int x = x0; x < maxX; ++x) {
                float fValue = sourceData.getElemFloatAt(sourceRaster.getDataBufferIndex(x, y));
                trgData.setElemFloatAt(targetTile.getDataBufferIndex(x, y), fValue);
            }
        }
    }

    /**
     * Double thresholding
     *
     * @param imageProcessor original image
     * @param highThreshold high threshold
     * @param lowThreshold low threshold
     * @return "trinarised" image
     */
    ImageProcessor trinarise(ByteProcessor imageProcessor, float highThreshold,
            float lowThreshold) {

        int width = imageProcessor.getWidth();
        int height = imageProcessor.getHeight();
        ImageProcessor returnedProcessor = imageProcessor.duplicate();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                float value = returnedProcessor.getPixelValue(x, y);

                if (value >= highThreshold) {
                    returnedProcessor.putPixel(x, y, 255);
                } else if (value >= lowThreshold) {
                    returnedProcessor.putPixel(x, y, 128);
                }
            }
        }
        return returnedProcessor;
    }

    /**
     * Hysteresis thresholding
     *
     * @param imageProcessor original image
     * @return thresholded image
     */
    ImageProcessor hysteresisThresholding(ByteProcessor imageProcessor) {

        int width = imageProcessor.getWidth();
        int height = imageProcessor.getHeight();

        ImageProcessor returnedProcessor = imageProcessor.duplicate();
        boolean change = true;

        while (change) {
            change = false;
            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                    if (returnedProcessor.getPixelValue(x, y) == MAX_VALUE - 1) {
                        if (returnedProcessor.getPixelValue(x + 1, y) == MAX_VALUE / 2) {
                            change = true;
                            returnedProcessor.putPixelValue(x + 1, y, MAX_VALUE - 1);
                        }
                        if (returnedProcessor.getPixelValue(x - 1, y) == MAX_VALUE / 2) {
                            change = true;
                            returnedProcessor.putPixelValue(x - 1, y, MAX_VALUE - 1);
                        }
                        if (returnedProcessor.getPixelValue(x, y + 1) == MAX_VALUE / 2) {
                            change = true;
                            returnedProcessor.putPixelValue(x, y + 1, MAX_VALUE - 1);
                        }
                        if (returnedProcessor.getPixelValue(x, y - 1) == MAX_VALUE / 2) {
                            change = true;
                            returnedProcessor.putPixelValue(x, y - 1, MAX_VALUE - 1);
                        }
                        if (returnedProcessor.getPixelValue(x + 1, y + 1) == MAX_VALUE / 2) {
                            change = true;
                            returnedProcessor.putPixelValue(x + 1, y + 1, MAX_VALUE - 1);
                        }
                        if (returnedProcessor.getPixelValue(x - 1, y - 1) == MAX_VALUE / 2) {
                            change = true;
                            returnedProcessor.putPixelValue(x - 1, y - 1, MAX_VALUE - 1);
                        }
                        if (returnedProcessor.getPixelValue(x - 1, y + 1) == MAX_VALUE / 2) {
                            change = true;
                            returnedProcessor.putPixelValue(x - 1, y + 1, MAX_VALUE - 1);
                        }
                        if (returnedProcessor.getPixelValue(x + 1, y - 1) == MAX_VALUE / 2) {
                            change = true;
                            returnedProcessor.putPixelValue(x + 1, y - 1, MAX_VALUE - 1);
                        }
                    }
                }
            }
            if (change) {
                for (int x = width - 2; x > 0; x--) {
                    for (int y = height - 2; y > 0; y--) {
                        if (returnedProcessor.getPixelValue(x, y) == MAX_VALUE - 1) {
                            if (returnedProcessor.getPixelValue(x + 1, y) == MAX_VALUE / 2) {
                                change = true;
                                returnedProcessor.putPixelValue(x + 1, y, MAX_VALUE - 1);
                            }
                            if (returnedProcessor.getPixelValue(x - 1, y) == MAX_VALUE / 2) {
                                change = true;
                                returnedProcessor.putPixelValue(x - 1, y, MAX_VALUE - 1);
                            }
                            if (returnedProcessor.getPixelValue(x, y + 1) == MAX_VALUE / 2) {
                                change = true;
                                returnedProcessor.putPixelValue(x, y + 1, MAX_VALUE - 1);
                            }
                            if (returnedProcessor.getPixelValue(x, y - 1) == MAX_VALUE / 2) {
                                change = true;
                                returnedProcessor.putPixelValue(x, y - 1, MAX_VALUE - 1);
                            }
                            if (returnedProcessor.getPixelValue(x + 1, y + 1) == MAX_VALUE / 2) {
                                change = true;
                                returnedProcessor.putPixelValue(x + 1, y + 1, MAX_VALUE - 1);
                            }
                            if (returnedProcessor.getPixelValue(x - 1, y - 1) == MAX_VALUE / 2) {
                                change = true;
                                returnedProcessor.putPixelValue(x - 1, y - 1, MAX_VALUE - 1);
                            }
                            if (returnedProcessor.getPixelValue(x - 1, y + 1) == MAX_VALUE / 2) {
                                change = true;
                                returnedProcessor.putPixelValue(x - 1, y + 1, MAX_VALUE - 1);
                            }
                            if (returnedProcessor.getPixelValue(x + 1, y - 1) == MAX_VALUE / 2) {
                                change = true;
                                returnedProcessor.putPixelValue(x + 1, y - 1, MAX_VALUE - 1);
                            }
                        }
                    }
                }
            }
        }
        // suppression
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (returnedProcessor.getPixelValue(x, y) == MAX_VALUE / 2) {
                    returnedProcessor.putPixelValue(x, y, MIN_VALUE);
                }
            }
        }
        return returnedProcessor;
    }

    /**
     * Get source tile rectangle.
     *
     * @param x0 X coordinate of the upper left corner point of the target tile
     * rectangle.
     * @param y0 Y coordinate of the upper left corner point of the target tile
     * rectangle.
     * @param w The width of the target tile rectangle.
     * @param h The height of the target tile rectangle.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(int x0, int y0, int w, int h) {

        int sx0 = x0;
        int sy0 = y0;
        int sw = w;
        int sh = h;

        if (x0 >= halfSizeX) {
            sx0 -= halfSizeX;
            sw += halfSizeX;
        }

        if (y0 >= halfSizeY) {
            sy0 -= halfSizeY;
            sh += halfSizeY;
        }

        if (x0 + w + halfSizeX <= sourceImageWidth) {
            sw += halfSizeX;
        }

        if (y0 + h + halfSizeY <= sourceImageHeight) {
            sh += halfSizeY;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }

    /**
     * The SPI is used to register this operator in the graph processing
     * framework via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}. This
     * class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(HysteresisThresholdingOp.class);
            setOperatorUI(HysteresisThresholdingOpUI.class);
        }
    }
}
