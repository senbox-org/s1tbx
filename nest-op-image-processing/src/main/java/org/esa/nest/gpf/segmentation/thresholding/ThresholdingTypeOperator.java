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
package org.esa.nest.gpf.segmentation.thresholding;

import com.bc.ceres.core.ProgressMonitor;
import ij.ImagePlus;
import ij.plugin.ContrastEnhancer;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.Tile;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Map;

/**
 * ThresholdingTypeOperator the description of all the thresholding methods
 * Hysteresis, MaximumEntropy, MixtureModeling & Otsu
 *
 * @author Emanuela Boros
 * @since October 2012
 */
public enum ThresholdingTypeOperator implements ThresholdingMethodEnforcement {

    Hysteresis {
        private float highThreshold = 100f;
        private float lowThreshold = 10f;
        final static int MAX_VALUE = 256;
        final static int MIN_VALUE = 0;

        public void setHighThreshold(float highThreshold) {
            this.highThreshold = highThreshold;
        }

        public void setLowThreshold(float lowThreshold) {
            this.lowThreshold = lowThreshold;
        }

        @Override
        public ByteProcessor computeThresholdingOperator(final Band sourceBand, final Tile sourceRaster,
                final Tile targetTile, final int x0, final int y0, final int w, final int h,
                final ProgressMonitor pm, Map<String, Object> paramMap) {

            setHighThreshold((Float) paramMap.get("highThreshold"));
            setLowThreshold((Float) paramMap.get("lowThreshold"));

            final RenderedImage fullRenderedImage = sourceBand.getSourceImage().getImage(0);
            final BufferedImage fullBufferedImage = new BufferedImage(sourceBand.getSceneRasterWidth(),
                    sourceBand.getSceneRasterHeight(),
                    BufferedImage.TYPE_USHORT_GRAY);
            fullBufferedImage.setData(fullRenderedImage.getData());

            ImagePlus fullImagePlus = new ImagePlus(sourceBand.getDisplayName(), fullBufferedImage);

            final ImageProcessor fullImageProcessor = fullImagePlus.getProcessor();
            ByteProcessor fullByteProcessor = (ByteProcessor) fullImageProcessor.convertToByte(true);
            ContrastEnhancer contrastEnhancer = new ContrastEnhancer();
            contrastEnhancer.equalize(fullByteProcessor);
            fullImagePlus.setProcessor(fullByteProcessor);

            return getThresholdedImage(fullByteProcessor);
        }

        @Override
        public ByteProcessor getThresholdedImage(ByteProcessor byteProcessor) {
            byteProcessor = (ByteProcessor) trinarise(byteProcessor, highThreshold, lowThreshold);
            byteProcessor = (ByteProcessor) hysteresisThresholding(byteProcessor);
            return byteProcessor;
        }

        /**
         * Double thresholding
         *
         * @param imageProcessor original image
         * @param highThreshold high threshold
         * @param lowThreshold low threshold
         * @return trinarised image
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
        public ImageProcessor hysteresisThresholding(ByteProcessor imageProcessor) {

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
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (returnedProcessor.getPixelValue(x, y) == MAX_VALUE / 2) {
                        returnedProcessor.putPixelValue(x, y, MIN_VALUE);
                    }
                }
            }
            return returnedProcessor;
        }
    },
    MaximumEntropy {
        @Override
        public ByteProcessor computeThresholdingOperator(final Band sourceBand, final Tile sourceRaster,
                final Tile targetTile, final int x0, final int y0, final int w, final int h,
                final ProgressMonitor pm, Map<String, Object> paramMap) {
            final RenderedImage fullRenderedImage = sourceBand.getSourceImage().getImage(0);
            final BufferedImage fullBufferedImage = new BufferedImage(sourceBand.getSceneRasterWidth(),
                    sourceBand.getSceneRasterHeight(),
                    BufferedImage.TYPE_USHORT_GRAY);
            fullBufferedImage.setData(fullRenderedImage.getData());

            ImagePlus fullImagePlus = new ImagePlus(sourceBand.getDisplayName(), fullBufferedImage);

            final ImageProcessor fullImageProcessor = fullImagePlus.getProcessor();

            ByteProcessor fullByteProcessor = (ByteProcessor) fullImageProcessor.convertToByte(true);
            ContrastEnhancer contrastEnhancer = new ContrastEnhancer();
            contrastEnhancer.equalize(fullByteProcessor);
            fullImagePlus.setProcessor(fullByteProcessor);

            fullByteProcessor = getThresholdedImage(fullByteProcessor);

            return fullByteProcessor;
        }

        @Override
        public ByteProcessor getThresholdedImage(ByteProcessor byteProcessor) {
//            int[] hist = byteProcessor.getHistogram();
//            int threshold = entropySplit(hist);
            byteProcessor = (ByteProcessor) maximumEntropyThresholding(byteProcessor);
//            byteProcessor.setAutoThreshold(AutoThresholder.Method.MaxEntropy, false,
//                    ImageProcessor.RED_LUT);
//            byteProcessor.autoThreshold();
            return byteProcessor;
        }

        /**
         * Hysteresis thresholding
         *
         * @param imageProcessor original image
         * @return thresholded image
         */
        ImageProcessor maximumEntropyThresholding(ByteProcessor imageProcessor) {

            ImageProcessor returnedProcessor = imageProcessor.duplicate();

            int[] hist = imageProcessor.getHistogram();
            int threshold = entropySplit(hist);
            returnedProcessor.threshold(threshold);
            return returnedProcessor;
        }

        /**
         * Calculate maximum entropy split of a histogram. Implements
         * Kapur-Sahoo-Wong (Maximum Entropy) thresholding method Kapur J.N.,
         * Sahoo P.K., and Wong A.K.C. (1985) "A New Method for Gray-Level
         * Picture Thresholding Using the Entropy of the Histogram" Graphical
         * Models and Image Processing, 29(3): 273-285 M. Emre Celebi 06.15.2007
         * Ported to ImageJ plugin by G.Landini from E Celebi's fourier_0.8
         * routines
         *
         * @param data histogram to be thresholded.
         *
         * @return index of the maximum entropy split.`
         */
        private int entropySplit(int[] data) {

            int threshold = -1;
            int firstBin;
            int lastBin;
            double totalEntropy; // total entropy
            double maximumEntropy; // max entropy
            double backgroundEntropy; // entropy of the background pixels at a given threshold
            double objectEntropy; // entropy of the object pixels at a given threshold
            double[] norm_histo = new double[data.length]; // normalized histogram
            double[] P1 = new double[data.length]; // cumulative normalized histogram

            double[] P2 = new double[data.length];

            int total = 0;
            for (int ih = 0; ih < data.length; ih++) {
                total += data[ih];
            }

            for (int ih = 0; ih < data.length; ih++) {
                norm_histo[ih] = (double) data[ih] / total;
            }

            P1[0] = norm_histo[0];
            P2[0] = 1.0 - P1[0];
            for (int ih = 1; ih < data.length; ih++) {
                P1[ih] = P1[ih - 1] + norm_histo[ih];
                P2[ih] = 1.0 - P1[ih];
            }

            /*
             * Determine the first non-zero bin
             */
            firstBin = 0;
            for (int ih = 0; ih < data.length; ih++) {
                if (!(Math.abs(P1[ih]) < 2.220446049250313E-16)) {
                    firstBin = ih;
                    break;
                }
            }

            /*
             * Determine the last non-zero bin
             */
            lastBin = data.length - 1;
            for (int ih = data.length - 1; ih >= firstBin; ih--) {
                if (!(Math.abs(P2[ih]) < 2.220446049250313E-16)) {
                    lastBin = ih;
                    break;
                }
            }

            /**
             * Calculate the total entropy each gray-level and find the
             * threshold that maximizes it
             */
            maximumEntropy = Double.MIN_VALUE;

            for (int it = firstBin; it <= lastBin; it++) {
                /*
                 * Entropy of the background pixels
                 */
                backgroundEntropy = 0.0;
                for (int ih = 0; ih <= it; ih++) {
                    if (data[ih] != 0) {
                        backgroundEntropy -= (norm_histo[ih] / P1[it]) * Math.log(norm_histo[ih] / P1[it]);
                    }
                }

                /*
                 * Entropy of the object pixels
                 */
                objectEntropy = 0.0;
                for (int ih = it + 1; ih < data.length; ih++) {
                    if (data[ih] != 0) {
                        objectEntropy -= (norm_histo[ih] / P2[it]) * Math.log(norm_histo[ih] / P2[it]);
                    }
                }

                /*
                 * Total entropy
                 */
                totalEntropy = backgroundEntropy + objectEntropy;

                if (maximumEntropy < totalEntropy) {
                    maximumEntropy = totalEntropy;
                    threshold = it;
                }
            }
            return threshold;
        }
    },
    MixtureModeling {
        int MAX_VALUE = 0;
        int MIN_VALUE = 256;
        public int N;
        public int[] probabilityHistogram;

        @Override
        public ByteProcessor computeThresholdingOperator(final Band sourceBand, final Tile sourceRaster,
                final Tile targetTile, final int x0, final int y0, final int w, final int h,
                final ProgressMonitor pm, Map<String, Object> paramMap) {

            final RenderedImage fullRenderedImage = sourceBand.getSourceImage().getImage(0);
            final BufferedImage fullBufferedImage = new BufferedImage(sourceBand.getSceneRasterWidth(),
                    sourceBand.getSceneRasterHeight(),
                    BufferedImage.TYPE_USHORT_GRAY);
            fullBufferedImage.setData(fullRenderedImage.getData());

            ImagePlus fullImagePlus = new ImagePlus(sourceBand.getDisplayName(), fullBufferedImage);
            final ImageProcessor fullImageProcessor = fullImagePlus.getProcessor();
            ByteProcessor fullByteProcessor = (ByteProcessor) fullImageProcessor.convertToByte(true);

            ContrastEnhancer contrastEnhancer = new ContrastEnhancer();
            contrastEnhancer.equalize(fullByteProcessor);
            fullImagePlus.setProcessor(fullByteProcessor);

            return getThresholdedImage(fullByteProcessor);
        }

        @Override
        public ByteProcessor getThresholdedImage(ByteProcessor byteProcessor) {

            int width = byteProcessor.getWidth();
            int height = byteProcessor.getHeight();

            N = width * height;

            GrayLevelClassMixtureModeling classes =
                    new GrayLevelClassMixtureModeling(byteProcessor);

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

            int threshold = findThreshold((int) mu1, (int) mu2, classes);
            byteProcessor.setThreshold(threshold + 1,
                    probabilityHistogram.length - 1, ImageProcessor.RED_LUT);
            return byteProcessor;
        }

        float calculateError(GrayLevelClassMixtureModeling classes) {
            float error = 0;

            for (int i = 0; i <= classes.getMAX(); i++) {
                error += Math.pow(classes.gamma(i) - classes.getHistogram(i), 2);
            }

            return (float) (error / (classes.getMAX() + 1));
        }

        class GrayLevelClassMixtureModeling {

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

        private int findThreshold(int mu1, int mu2,
                GrayLevelClassMixtureModeling classes) {

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
    },
    Otsu {
        private ImagePlus fullImagePlus;
        private ByteProcessor fullByteProcessor;

        @Override
        public ByteProcessor computeThresholdingOperator(final Band sourceBand, final Tile sourceRaster,
                final Tile targetTile, final int x0, final int y0, final int w, final int h,
                final ProgressMonitor pm, Map<String, Object> paramMap) {
            final RenderedImage fullRenderedImage = sourceBand.getSourceImage().getImage(0);
            final BufferedImage fullBufferedImage = new BufferedImage(sourceBand.getSceneRasterWidth(),
                    sourceBand.getSceneRasterHeight(),
                    BufferedImage.TYPE_USHORT_GRAY);
            fullBufferedImage.setData(fullRenderedImage.getData());

            fullImagePlus = new ImagePlus(sourceBand.getDisplayName(), fullBufferedImage);
            final ImageProcessor fullImageProcessor = fullImagePlus.getProcessor();
            fullByteProcessor = (ByteProcessor) fullImageProcessor.convertToByte(true);
            ContrastEnhancer contrastEnhancer = new ContrastEnhancer();
            contrastEnhancer.equalize(fullByteProcessor);
            fullImagePlus.setProcessor(fullByteProcessor);

            fullImagePlus.show();
            return getThresholdedImage(fullByteProcessor);
        }

        @Override
        public ByteProcessor getThresholdedImage(ByteProcessor byteProcessor) {
            byteProcessor.setAutoThreshold(AutoThresholder.Method.Otsu, false, ImageProcessor.RED_LUT);
            byteProcessor.autoThreshold();
            return byteProcessor;
        }
    };

    /**
     *
     * @param sourceBand
     * @param sourceRaster
     * @param targetTile
     * @param x0
     * @param y0
     * @param w
     * @param h
     * @param pm
     * @param paramMap
     * @return
     */
    @Override
    public abstract ByteProcessor computeThresholdingOperator(final Band sourceBand, final Tile sourceRaster,
            final Tile targetTile, final int x0, final int y0, final int w, final int h,
            final ProgressMonitor pm, Map<String, Object> paramMap);
}
