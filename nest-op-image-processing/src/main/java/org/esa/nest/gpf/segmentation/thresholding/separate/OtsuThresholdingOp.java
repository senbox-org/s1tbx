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
import ij.process.AutoThresholder;
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

@OperatorMetadata(alias = "OtsuThresholding",
        category = "Image Processing",
        authors = "Emanuela Boros",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "OtsuThresholdingOp")
public class OtsuThresholdingOp extends Operator {

    private static float[] probabilityHistogram;
    public static boolean probabilityHistogramDone;
    public static int N;
    @SourceProduct(alias = "source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;
    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
    rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;
    private final Map<String, String[]> targetBandNameToSourceBandName = new HashMap<String, String[]>();
    private int sourceImageWidth;
    private int sourceImageHeight;
    private static boolean processed = false;
    private int halfSizeX;
    private int halfSizeY;
    private int filterSizeX = 3;
    private int filterSizeY = 3;
    private static ImagePlus fullImagePlus;

    /**
     * Default constructor. The graph processing framework requires that an
     * operator has a default constructor.
     */
    public OtsuThresholdingOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product}
     * annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct}
     * annotation or by calling {@link #setTargetProduct} method.</p> <p>The
     * framework calls this method after it has created this operator. Any
     * client code that must be performed before computation of tile data should
     * be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs
     * during operator initialisation.
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

            computeOtsuThresholding(sourceBand, sourceRaster, targetTile, x0, y0, w, h, pm);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }
    /**
     * Apply Otsu Thresholding
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
    private static ImageProcessor fullByteProcessor;

    private synchronized void computeOtsuThresholding(final Band sourceBand, final Tile sourceRaster,
            final Tile targetTile, final int x0, final int y0, final int w, final int h,
            final ProgressMonitor pm) {

        if (!processed) {
            final RenderedImage fullRenderedImage = sourceBand.getSourceImage().getImage(0);
            final BufferedImage fullBufferedImage = new BufferedImage(sourceBand.getSceneRasterWidth(),
                    sourceBand.getSceneRasterHeight(),
                    BufferedImage.TYPE_USHORT_GRAY);
            fullBufferedImage.setData(fullRenderedImage.getData());

            fullImagePlus = new ImagePlus(sourceBand.getDisplayName(), fullBufferedImage);
//            JOptionPane.showMessageDialog(null, fullImagePlus.getBitDepth() + ", "
//                    + fullImagePlus.getProcessor().getCurrentColorModel(),
//                    "getImagePlus", JOptionPane.INFORMATION_MESSAGE);

            final ImageProcessor fullImageProcessor = fullImagePlus.getProcessor();

            ImageProcessor tmp1 = null;
            ImageStack stack = fullImagePlus.getStack();
            ImageStack res_trin = new ImageStack(stack.getWidth(), stack.getHeight());

            tmp1 = fullImageProcessor.convertToShort(true);
            for (int s = 1; s <= stack.getSize(); s++) {
//                tmp1 = ImageEdge.trin(stack.getProcessor(s), highThreshold, lowThreshold);
                tmp1.setAutoThreshold(AutoThresholder.Method.Otsu, false, ImageProcessor.RED_LUT);
                tmp1.autoThreshold();
                res_trin.addSlice("", tmp1);
            }
            fullByteProcessor = new ImagePlus("Hysteresis", res_trin).getProcessor();//.convertToShort(true);
//            fullByteProcessor = (ByteProcessor) new ImagePlus("Hysteresis", res_trin).getProcessor().convertToByte(true);

//            fullByteProcessor = (ByteProcessor) fullImageProcessor.convertToByte(true);
//            fullByteProcessor.setAutoThreshold(AutoThresholder.Method.Otsu, false, ImageProcessor.RED_LUT);
//            fullByteProcessor.autoThreshold();
            processed = true;
        }

        final ImageProcessor fullImageProcessor = fullImagePlus.getProcessor();

//        ByteProcessor fullByteProcessor = (ByteProcessor) fullImageProcessor.convertToByte(true);

//        int width = fullByteProcessor.getWidth();
//        int height = fullByteProcessor.getHeight();
//
//        int intMax = (int) fullByteProcessor.getMax();
//
//        N = width * height;
//        probabilityHistogramDone = false;
//
//        GrayLevel grayLevelTrue =
//                new GrayLevel(fullByteProcessor, true);
//        GrayLevel grayLevelFalse =
//                new GrayLevel(fullByteProcessor, false);
//
//        float fullMu = (float) grayLevelTrue.getOmega() * grayLevelTrue.getMu()
//                + (float) grayLevelFalse.getOmega() * grayLevelFalse.getMu();
//
//        double sigmaMax = 0d;
//        float threshold = 0f;
//
//        for (int i = 0; i < intMax; i++) {
//
//            double sigma = (double) grayLevelTrue.getOmega() * (Math.pow(grayLevelTrue.getMu() - fullMu, 2))
//                    + (double) grayLevelFalse.getOmega()
//                    * (Math.pow(grayLevelFalse.getMu() - fullMu, 2));
//
//            if (sigma > sigmaMax) {
//                sigmaMax = sigma;
//                threshold = grayLevelTrue.getThreshold();
//            }
//
//            grayLevelTrue.addToEnd();
//            grayLevelFalse.removeFromBeginning();
//        }

        final Rectangle srcTileRectangle = sourceRaster.getRectangle();

        fullByteProcessor.setRoi(srcTileRectangle);

        ImageProcessor roiImageProcessor = fullByteProcessor.crop();

        final ProductData trgData = targetTile.getDataBuffer();
        final ProductData sourceData = ProductData.createInstance((short[]) roiImageProcessor.getPixels());

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
     * Estimate the threshold for the given image. (OpenImaj)
     *
     * @param img the image
     * @return the estimated threshold
     */
    public float calculateThreshold(ByteProcessor fullByteProcessor) {
        if (!probabilityHistogramDone) {
            int[] histogram = fullByteProcessor.getHistogram();
            int length = histogram.length;
            probabilityHistogram = new float[length];

            for (int i = 0; i < length; i++) {
                probabilityHistogram[i] = ((float) histogram[i]) / ((float) N);
            }
            probabilityHistogramDone = true;
        }

        // Total number of pixels
        int total = fullByteProcessor.getWidth() * fullByteProcessor.getHeight();

        float sum = 0;
        for (int t = 0; t < 256; t++) {
            sum += t * probabilityHistogram[t];
        }

        float sumB = 0;
        int wB = 0;
        int wF = 0;

        float varMax = 0;
        float threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += probabilityHistogram[t];               // Weight Background
            if (wB == 0) {
                continue;
            }

            wF = total - wB;                 // Weight Foreground
            if (wF == 0) {
                break;
            }

            sumB += (t * probabilityHistogram[t]);

            float mB = sumB / wB;            // Mean Background
            float mF = (sum - sumB) / wF;    // Mean Foreground

            // Calculate Between Class Variance
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            // Check if new maximum found
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }

        return threshold / 255;
    }

    /**
     * Thresholding
     *
     * @param imageProcessor original image
     * @param highThreshold high threshold
     * @param lowThreshold low threshold
     * @return "thresholded" image
     */
    ImageProcessor threshold(ImageProcessor imageProcessor, float threshold) {

        int width = imageProcessor.getWidth();
        int height = imageProcessor.getHeight();
        ImageProcessor returnedProcessor = imageProcessor.duplicate();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                float value = returnedProcessor.getPixelValue(x, y);
//0xFF &
                if (value <= threshold) {
                    returnedProcessor.putPixelValue(x, y, 0);
                } else {
                    returnedProcessor.putPixelValue(x, y, (byte) 255);
                }
            }
        }
        return returnedProcessor;
    }

    private class GrayLevel {

        private int index;
        private float omega;
        private float mu;

        public GrayLevel(ImageProcessor imageProcessor, boolean isFirst) {

            if (!probabilityHistogramDone) {
                int[] histogram = imageProcessor.getHistogram();
                int length = histogram.length;
                probabilityHistogram = new float[length];

                for (int i = 0; i < length; i++) {
                    probabilityHistogram[i] = ((float) histogram[i]) / ((float) N);
                }
                probabilityHistogramDone = true;
            }

            if (isFirst) {
                index = 1;
                omega = probabilityHistogram[index - 1];
                if (omega == 0) {
                    mu = 0;
                } else {
                    mu = 1 * probabilityHistogram[index - 1] / omega;
                }
            } else {
                index = 2;
                omega = 0;
                mu = 0;
                for (int i = index; i < probabilityHistogram.length; i++) {
                    omega += probabilityHistogram[i - 1];
                    mu += probabilityHistogram[i - 1] * i;
                }
                if (omega == 0) {
                    mu = 0;
                } else {
                    mu /= omega;
                }
            }
        }

        public void removeFromBeginning() {
            index++;
            mu = 0;
            omega = 0;

            for (int i = index; i < probabilityHistogram.length; i++) {
                omega += probabilityHistogram[i - 1];
                mu += i * probabilityHistogram[i - 1];//i*
            }
            if (omega == 0) {
                mu = 0;
            } else {
                mu /= omega;
            }
        }

        public void addToEnd() {
            index++;
            mu = 0;
            omega = 0;
            for (int i = 1; i < index; i++) {
                omega += probabilityHistogram[i - 1];
                mu += i * probabilityHistogram[i - 1];
            }
            if (omega == 0) {
                mu = 0;
            } else {
                mu /= omega;
            }
        }

        public float getMu() {
            return mu;
        }

        public float getOmega() {
            return omega;
        }

        public int getThreshold() {
            return index;
        }
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
            super(OtsuThresholdingOp.class);
            setOperatorUI(OtsuThresholdingOpUI.class);
        }
    }
}
