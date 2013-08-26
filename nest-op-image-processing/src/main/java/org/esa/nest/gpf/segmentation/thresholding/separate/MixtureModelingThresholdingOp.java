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
import ij.gui.NewImage;
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

@OperatorMetadata(alias = "MixtureModelingThresholding",
        category = "Image Processing",
        authors = "Emanuela Boros",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "MixtureModelingThresholding")
public class MixtureModelingThresholdingOp extends Operator {

    public static int[] probabilityHistogram;
    private int threshold;
    final static int MAX_VALUE = 0;
    final static int MIN_VALUE = 256;
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

            computeMixtureModelingThresholding(sourceBand, sourceRaster, targetTile, x0, y0, w, h, pm);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Apply MixtureModelingThresholding
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
    private synchronized void computeMixtureModelingThresholding(final Band sourceBand, final Tile sourceRaster,
            final Tile targetTile, final int x0, final int y0, final int w, final int h,
            final ProgressMonitor pm) {

        if (!processed) {
            final RenderedImage fullRenderedImage = sourceBand.getSourceImage().getImage(0);
            final BufferedImage fullBufferedImage = new BufferedImage(sourceBand.getSceneRasterWidth(),
                    sourceBand.getSceneRasterHeight(),
                    BufferedImage.TYPE_USHORT_GRAY);
            fullBufferedImage.setData(fullRenderedImage.getData());

            fullImagePlus = new ImagePlus(sourceBand.getDisplayName(), fullBufferedImage);
            processed = true;
        }

        final ImageProcessor fullImageProcessor = fullImagePlus.getProcessor();

        ByteProcessor fullByteProcessor = (ByteProcessor) fullImageProcessor.convertToByte(true);

        int width = fullByteProcessor.getWidth();
        int height = fullByteProcessor.getHeight();

        N = width * height;

        GrayLevelClassMixtureModeling classes = new GrayLevelClassMixtureModeling(fullByteProcessor);

        int foundThreshold = 0;

        float error = 0;
        float errorMin = Float.MAX_VALUE;
        float mu1 = 0, mu2 = 0;

        while (classes.addToIndex()) {
            error = calculateError(classes);
            if (error < errorMin) {
                errorMin = error;
                foundThreshold = classes.getThreshold();
                mu1 = classes.getMu1();
                mu2 = classes.getMu2();
            }
        }
        classes.setIndex(foundThreshold);

        threshold = findThreshold((int) mu1, (int) mu2, classes);

        final Rectangle srcTileRectangle = sourceRaster.getRectangle();

        fullByteProcessor.setRoi(srcTileRectangle);

        ImageProcessor roiImageProcessor = fullByteProcessor.crop();
        ImagePlus imp = null;
        imp = NewImage.createByteImage("Threshold", roiImageProcessor.getWidth(),
                roiImageProcessor.getHeight(), 1, NewImage.FILL_WHITE);

        ImageProcessor nip = imp.getProcessor();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int value = roiImageProcessor.getPixel(x, y);
                if (value > threshold) {
                    nip.putPixel(x, y, 255);
                } else {
                    nip.putPixel(x, y, 0);
                }
            }
        }

        final ProductData trgData = targetTile.getDataBuffer();
        final ProductData sourceData = ProductData.createInstance((byte[]) nip.getPixels());

        final int maxY = y0 + h;
        final int maxX = x0 + w;
        for (int y = y0; y < maxY; ++y) {
            for (int x = x0; x < maxX; ++x) {
                float fValue = sourceData.getElemFloatAt(sourceRaster.getDataBufferIndex(x, y));
                trgData.setElemFloatAt(targetTile.getDataBufferIndex(x, y), fValue);
            }
        }
    }

    private int findThreshold(int mu1, int mu2, GrayLevelClassMixtureModeling classes) {

        float min = Float.MAX_VALUE;
        int foundThreshold = 0;

        for (int i = mu1; i < mu2; i++) {
            float val = (float) Math.pow(classes.differenceGamma(i), 2);
            if (min > val) {
                min = val;
                foundThreshold = i;
            }
        }
        return foundThreshold;
    }

    private float calculateError(GrayLevelClassMixtureModeling classes) {
        float error = 0;

        for (int i = 0; i <= classes.getMAX(); i++) {
            error += Math.pow(classes.gamma(i) - classes.getHistogram(i), 2);
        }

        return (float) (error / (classes.getMAX() + 1));
    }

    private class GrayLevelClassMixtureModeling {

        private int index;
        private float mu1, mu2;
        private float sigma2_1, sigma2_2;
        private float mult1, mult2;
        private float twoVariance1, twoVariance2;
        private float max1, max2;
        private int cardinal1, cardinal2;
        private int cardinal;
        private int MIN_INDEX = 1;
        private int MAX_INDEX = 253;
        private int MIN = 0;
        private final int MAX = 255;

        public GrayLevelClassMixtureModeling(ImageProcessor imageProcessor) {

            cardinal = imageProcessor.getWidth() * imageProcessor.getHeight();

//            if (!probabilityHistogramDone) {
//                int[] histogram = imageProcessor.getHistogram();
//                int length = histogram.length;
//                probabilityHistogram = new float[length];
//
//                for (int i = 0; i < length; i++) {
//                    probabilityHistogram[i] = ((float) histogram[i]) / ((float) N);
//                }
//                probabilityHistogramDone = true;
//            }
            probabilityHistogram = imageProcessor.getHistogram();
            index = MIN_INDEX - 1;
        }

        public boolean addToIndex() {

            index++;
            if (!(index <= MAX_INDEX)) {
                return false;
            }
            resetValues();
            return true;
        }

        private float calculateMax(int index) {

            float sum = probabilityHistogram[index];
            float num = 1;
            if (index - 1 >= 0) {
                sum += probabilityHistogram[index - 1];
                num++;
            }
            if (index + 1 < MAX) {
                sum += probabilityHistogram[index + 1];
                num++;
            }
            return (float) (sum / num);
        }

        public float getCardinal() {
            return cardinal;
        }

        public float getMu1() {
            return mu1;
        }

        public float getMu2() {
            return mu2;
        }

        public float getMax1() {
            return max1;
        }

        public float getMax2() {
            return max2;
        }

        public float getVariance1() {
            return sigma2_1;
        }

        public float getVariance2() {
            return sigma2_2;
        }

        public float getCardinal1() {
            return cardinal1;
        }

        public float getCardinal2() {
            return cardinal2;
        }

        public int getThreshold() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
            resetValues();
        }

        private void resetValues() {

            mu1 = 0;
            mu2 = 0;
            sigma2_1 = 0;
            sigma2_2 = 0;
            max1 = 0;
            max2 = 0;
            cardinal1 = 0;
            cardinal2 = 0;

            for (int i = MIN; i <= index; i++) {
                cardinal1 += probabilityHistogram[i];
                mu1 += i * probabilityHistogram[i];
            }

            for (int i = index + 1; i <= MAX; i++) {
                cardinal2 += probabilityHistogram[i];
                mu2 += i * probabilityHistogram[i];
            }

            if (cardinal1 == 0) {
                mu1 = 0;
                sigma2_1 = 0;
            } else {
                mu1 /= (float) cardinal1;
            }

            if (cardinal2 == 0) {
                mu2 = 0;
                sigma2_2 = 0;
            } else {
                mu2 /= (float) cardinal2;
            }

            if (mu1 != 0) {
                for (int i = MIN; i <= index; i++) {
                    sigma2_1 += probabilityHistogram[i] * Math.pow(i - mu1, 2);
                }

                sigma2_1 /= (float) cardinal1;

                max1 = calculateMax((int) mu1);

                mult1 = (float) max1;
                twoVariance1 = 2 * sigma2_1;
            }
            if (mu2 != 0) {
                for (int i = index + 1; i <= MAX; i++) {
                    sigma2_2 += probabilityHistogram[i] * Math.pow(i - mu2, 2);
                }

                sigma2_2 /= (float) cardinal2;

                max2 = calculateMax((int) mu2);

                mult2 = (float) max2;
                twoVariance2 = 2 * sigma2_2;
            }
        }

        public final float gamma1(int i) {
            if (sigma2_1 == 0) {
                return 0;
            }
            return (float) (mult1 * Math.exp(-(Math.pow((float) i - mu1, 2)) / twoVariance1));
        }

        public final float gamma2(int i) {
            if (sigma2_2 == 0) {
                return 0;
            }
            return (float) (mult2 * Math.exp(-(Math.pow((float) i - mu2, 2)) / twoVariance2));
        }

        public float gamma(int i) {
            return gamma1(i) + gamma2(i);
        }

        public float differenceGamma(int i) {
            return gamma1(i) - gamma2(i);
        }

        public int getHistogram(int i) {
            return probabilityHistogram[i];
        }

        /**
         * @return the MAX
         */
        public int getMAX() {
            return MAX;
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
            super(MixtureModelingThresholdingOp.class);
            setOperatorUI(MixtureModelingThresholdingOpUI.class);
        }
    }
}
