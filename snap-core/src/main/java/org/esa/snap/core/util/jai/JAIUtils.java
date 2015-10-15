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
package org.esa.snap.core.util.jai;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.IntMap;

import javax.media.jai.DataBufferFloat;
import javax.media.jai.Histogram;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.TiledImage;
import javax.media.jai.WarpPolynomial;
import javax.media.jai.operator.ClampDescriptor;
import javax.media.jai.operator.CompositeDescriptor;
import javax.media.jai.operator.LookupDescriptor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A collection of global JAI functions allowing a type-safe usage of various JAI imaging operators.
 */
public class JAIUtils {

    static final int TILE_SIZE_STEP = 64;
    static final int MIN_TILE_SIZE = 4 * TILE_SIZE_STEP;
    static final int MAX_TILE_SIZE = 10 * TILE_SIZE_STEP;

    /**
     * Sets the memory capacity of the default tile cache in megabytes
     *
     * @param megabytes the memory capacity in megabytes
     */
    public static void setDefaultTileCacheCapacity(int megabytes) {
        final TileCache tileCache = JAI.getDefaultInstance().getTileCache();
        // JAIJAIJAI
        tileCache.memoryControl();
        tileCache.setMemoryCapacity(megabytes * 1024L * 1024L);
        Debug.trace("JAI tile cache capacity set to " + tileCache.getMemoryCapacity() + " bytes");
    }

    public static RenderedOp createTileFormatOp(RenderedImage img, int tileWidth, int tileHeight) {
        ImageLayout tileLayout = new ImageLayout(img);
        tileLayout.setTileWidth(tileWidth);
        tileLayout.setTileHeight(tileHeight);

        HashMap map = new HashMap();
        map.put(JAI.KEY_IMAGE_LAYOUT, tileLayout);
        RenderingHints tileHints = new RenderingHints(map);

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(img);
        return JAI.create("format", pb, tileHints);
    }

    public static TiledImage createTiledImage(float[] data,
                                              int width,
                                              int height) {
        return createTiledImage(new float[][]{data},
                                width,
                                height,
                                1);
    }

    public static TiledImage createTiledImage(float[][] data,
                                              int width,
                                              int height,
                                              int numBands) {
        int length = width * height;

        // create a float sample model
        SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT,
                                                                        width,
                                                                        height,
                                                                        numBands);

        // create a DataBuffer from the float[][] data array
        DataBufferFloat dataBuffer = new DataBufferFloat(data, length);

        // create a Raster
        Point origin = new Point(0, 0);
        Raster raster = RasterFactory.createWritableRaster(sampleModel,
                                                           dataBuffer,
                                                           origin);
        return createTiledImage(raster);
    }


    /**
     * Creates a tiled image from the supplied raster. Note that this method allocates a new raster for the tiled image
     * returned. The data of the given raster is then copied into the new one.
     */
    public static TiledImage createTiledImage(Raster raster) {

        // create a float sample model
        SampleModel sampleModel = raster.getSampleModel();

        // create a compatible ColorModel
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

        // create a TiledImage using the float SampleModel
        TiledImage tiledImage = new TiledImage(0,
                                               0,
                                               raster.getWidth(),
                                               raster.getHeight(),
                                               0,
                                               0,
                                               sampleModel,
                                               colorModel);

        // set the TiledImage data to that of the Raster
        tiledImage.setData(raster);
        JAIDebug.trace("createRenderedImage.tiledImage", tiledImage);

        return tiledImage;
    }

    public static PlanarImage createPlanarImage(WritableRaster raster) {
        ColorModel cm = PlanarImage.createColorModel(raster.getSampleModel());
        BufferedImage bi = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
        return PlanarImage.wrapRenderedImage(bi);
    }

    /**
     * Use this method to scale an CS_sRGB into a grayscale Image (CS_GRAY)
     */
    public static RenderedOp createColorToGrayOp(RenderedImage src) {

        if (src.getColorModel().getNumColorComponents() != 3) {
            throw new IllegalArgumentException("a minimum of three bands expected");
        }

        double[][] matrix = {{0.114, 0.587, 0.299, 0.0}};

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);
        pb.add(matrix);
        return JAI.create("bandcombine", pb, null);
    }


    public static double[][] getExtrema(RenderedImage src) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);
        RenderedOp op = JAI.create("extrema", pb, null);
        return (double[][]) op.getProperty("extrema");
    }

    public static double[] getExtrema(RenderedImage src,
                                      double[] extrema) {
        double[][] extr = getExtrema(src);
        if (extrema == null) {
            extrema = new double[2];
        }
        extrema[0] = extr[0][0];
        extrema[1] = extr[1][0];
        return extrema;
    }


    public static RenderedOp createScaleOp(RenderedImage src,
                                           double xScale,
                                           double yScale,
                                           double xTrans,
                                           double yTrans,
                                           Interpolation ip) {
        // Create a ParameterBlock and specify the source and
        // parameters
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);                   // The source image
        pb.add((float) xScale);                        // The xScale
        pb.add((float) yScale);                        // The yScale
        pb.add((float) xTrans);                       // The x translation
        pb.add((float) yTrans);                       // The y translation
        pb.add(ip); // The interpolation

        // Create the scale operation
        return JAI.create("scale", pb, null);
    }


    public static RenderedOp createRectifyOp(RenderedImage src,
                                             int degree,
                                             RectificationGrid grid,
                                             Interpolation interp) {

        WarpPolynomial warp = WarpPolynomial.createWarp(grid.sourceCoords,
                                                        0,
                                                        grid.destCoords,
                                                        0,
                                                        grid.numCoords,
                                                        1.0F,
                                                        1.0F,
                                                        1.0F,
                                                        1.0F,
                                                        degree);

        if (interp == null) {
            interp = new InterpolationNearest();
        }

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);
        pb.add(warp);
        pb.add(interp);
        return JAI.create("warp", pb);
    }

    public static RenderedOp createRescaleOp(RenderedImage src,
                                             double scale,
                                             double offset) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);
        pb.add(new double[]{scale});
        pb.add(new double[]{offset});
        return JAI.create("rescale", pb, null);
    }


    public static RenderedOp createFormatOp(RenderedImage src, int dataType) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);
        pb.add(dataType);

        RenderingHints rh = null;
        if (dataType == DataBuffer.TYPE_BYTE) {
            /*
             * @todo 3 nf/nf - make the color model a parameter
             */

            /*
             * @todo 3 nf/nf - what about this comment
             */
            ColorModel cm = ImageUtils.create8BitGreyscaleColorModel();
            SampleModel sm = cm.createCompatibleSampleModel(src.getTileWidth(),
                                                            src.getTileHeight());
            ImageLayout layout = new ImageLayout(src);
            layout.setColorModel(cm);
            layout.setSampleModel(sm);

            rh = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        } else {
        }

        return JAI.create("format", pb, rh);
    }

    public static RenderedOp createClampOp(RenderedImage src,
                                           double minValue,
                                           double maxValue) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);
        pb.add(new double[]{minValue});
        pb.add(new double[]{maxValue});
        return JAI.create("clamp", pb, null);
    }

    public static RenderedOp createLookupOp(RenderedImage src, byte[][] lookupTable) {
        LookupTableJAI lookup = new LookupTableJAI(lookupTable);
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);
        pb.add(lookup);
        return JAI.create("lookup", pb, null);
    }

    public static RenderedOp createLogOp(RenderedImage src) {
        return JAI.create("log", src);
    }

    public static RenderedOp createExpOp(RenderedImage src) {
        return JAI.create("exp", src);
    }

    public static RenderedOp createNullOp(RenderingHints rh) {
        ParameterBlock pb = new ParameterBlock();
        return JAI.create("null", pb, rh);
    }

    public static RenderedOp createRotateOp(RenderedImage src,
                                            double xOrigin,
                                            double yOrigin,
                                            double angle) {

        angle *= Math.PI / 180.0;

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);                  // The source image
        pb.add((float) xOrigin);            // The x origin
        pb.add((float) yOrigin);            // The y origin
        pb.add((float) angle);              // The rotation angle in RAD
        pb.add(new javax.media.jai.InterpolationNearest()); // The interpolation

        return JAI.create("rotate", pb, null);
    }

    public static RenderedOp createByteCastOp(RenderedImage src) {
        return createFormatOp(src, DataBuffer.TYPE_BYTE);
    }

    public static RenderedOp createShortCastOp(RenderedImage src) {
        return createFormatOp(src, DataBuffer.TYPE_SHORT);
    }

    public static RenderedOp createUShortCastOp(RenderedImage src) {
        return createFormatOp(src, DataBuffer.TYPE_USHORT);
    }

    public static RenderedOp createIntCastOp(RenderedImage src) {
        return createFormatOp(src, DataBuffer.TYPE_INT);
    }

    public static RenderedOp createStretchOp(RenderedImage src,
                                             double minValue,
                                             double maxValue) {
        return createStretchOp(src, minValue, maxValue, 0.0, 1.0);
    }

    public static RenderedOp createStretchOp(RenderedImage src,
                                             double minValueOld,
                                             double maxValueOld,
                                             double minValueNew,
                                             double maxValueNew) {
        double scale = (maxValueNew - minValueNew) / (maxValueOld - minValueOld);
        return createRescaleOp(src,
                               scale,
                               minValueNew - minValueOld * scale);
    }

    public static RenderedOp createValueToIndexOp(RenderedImage src,
                                                  double minValue,
                                                  double maxValue,
                                                  int numIndices) {
        double minValueNew = 0.0;
        double maxValueNew = numIndices;

        JAIDebug.trace("createValueToIndexOp.src", src);

        RenderedOp op1 = createStretchOp(src,
                                         minValue,
                                         maxValue,
                                         minValueNew,
                                         maxValueNew);

        JAIDebug.trace("createValueToIndexOp.op1", op1);

        RenderedOp op2 = createClampOp(op1,
                                       minValueNew,
                                       maxValueNew - 1.0);

        JAIDebug.trace("createValueToIndexOp.op2", op2);

        RenderedOp op3 = null;
        if (numIndices <= (1 + (int) Byte.MAX_VALUE - (int) Byte.MIN_VALUE)) {
            op3 = createByteCastOp(op2);
        } else if (numIndices <= (int) Short.MAX_VALUE) {
            op3 = createShortCastOp(op2);
        } else if (numIndices <= (1 + (int) Short.MAX_VALUE - (int) Short.MIN_VALUE)) {
            op3 = createUShortCastOp(op2);
        } else {
            op3 = createIntCastOp(op2);
        }

        JAIDebug.trace("createValueToIndexOp.op3", op2);
        return op3;
    }

    public static RenderedOp createValueToIndexOp(RenderedImage src,
                                                  int numIndices) {
        double[] extrema = getExtrema(src, null);
        return createValueToIndexOp(src, extrema[0], extrema[1], numIndices);
    }

    public static PlanarImage createPaletteOp(RenderedImage src,
                                              byte[] r,
                                              byte[] g,
                                              byte[] b) {

        int size = Math.min(Math.min(r.length, g.length), b.length);
        int bits = size <= 256 ? 8 : 16;

        ColorModel colorModel = new IndexColorModel(bits, size, r, g, b);

        ImageLayout layout = new ImageLayout();
        layout.setColorModel(colorModel);

        return new NullOpImage(src,
                               layout,
                               null,
                               OpImage.OP_COMPUTE_BOUND);
    }


    /**
     * Returns a rescaled version of the given source image in the range
     * <pre>
     *    low  = level - window / 2
     *    high = level + window / 2
     * </pre>
     * <p> The image returned is always of type 'byte' and has the same number of bands as the source image.
     *
     * @param image  the source image, can be of any type
     * @param window the range window
     * @param level  the range level
     *
     * @return a rescaled version of the source image
     */
    public static PlanarImage createWindowLevelImage(RenderedImage image,
                                                     double window,
                                                     double level) {
        Guardian.assertNotNull("image", image);
        double low = level - window / 2.0;
        double high = level + window / 2.0;
        return createRescaledImage(image, low, high);
    }

    /**
     * Returns a rescaled version of the given source image in the given sample value range. <p> The image returned is
     * always of type 'byte' and has the same number of bands as the source image.
     *
     * @param image the source image, can be of any type
     * @param low   the minimum value of the range
     * @param high  the maximum value of the range
     *
     * @return a rescaled version of the source image
     */
    public static PlanarImage createRescaledImage(RenderedImage image,
                                                  double low,
                                                  double high) {
        Guardian.assertNotNull("image", image);
        ParameterBlock pb = null;
        PlanarImage dst = null;
        int bands = image.getSampleModel().getNumBands();
        int dtype = image.getSampleModel().getDataType();
        double slope;
        double y_int;

        if (dtype == DataBuffer.TYPE_BYTE) {

            // use a lookup table for rescaling
            if (high != low) {
                slope = 256.0 / (high - low);
                y_int = 256.0 - slope * high;
            } else {
                slope = 0.0;
                y_int = 0.0;
            }

            // @todo 3 se/nf - (dpc1) duplicated code -> search (dpc2)
            byte[][] lut = new byte[bands][256];
            for (int j = 0; j < bands; j++) {
                byte[] lutb = lut[j];
                for (int i = 0; i < 256; i++) {
                    int value = (int) (slope * i + y_int);
                    if (value < (int) low) {
                        value = 0;
                    } else if (value > (int) high) {
                        value = 255;
                    } else {
                        value &= 0xFF;
                    }
                    lutb[i] = (byte) value;
                }
            }

            LookupTableJAI lookup = new LookupTableJAI(lut);
            pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(lookup);
            dst = JAI.create("lookup", pb, null);

        } else if (dtype == DataBuffer.TYPE_SHORT
                   || dtype == DataBuffer.TYPE_USHORT) {

            // use a lookup table for rescaling
            if (high != low) {
                slope = 256.0 / (high - low);
                y_int = 256.0 - slope * high;
            } else {
                slope = 0.0;
                y_int = 0.0;
            }

            // @todo 3 se/nf - (dpc2) duplicated code -> search (dpc1)
            byte[][] lut = new byte[bands][65536];
            for (int j = 0; j < bands; j++) {
                byte[] lutb = lut[j];
                for (int i = 0; i < 65535; i++) {
                    int value = (int) (slope * i + y_int);
                    if (dtype == DataBuffer.TYPE_USHORT) {
                        value &= 0xFFFF;
                    }
                    if (value < (int) low) {
                        value = 0;
                    } else if (value > (int) high) {
                        value = 255;
                    } else {
                        value &= 0xFF;
                    }
                    lutb[i] = (byte) value;
                }
            }

            LookupTableJAI lookup = new LookupTableJAI(lut);

            pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(lookup);
            dst = JAI.create("lookup", pb, null);

        } else if (dtype == DataBuffer.TYPE_INT
                   || dtype == DataBuffer.TYPE_FLOAT
                   || dtype == DataBuffer.TYPE_DOUBLE) {

            // use the rescale and format ops
            if (high != low) {
                slope = 256.0 / (high - low);
                y_int = 256.0 - slope * high;
            } else {
                slope = 0.0;
                y_int = 0.0;
            }

            dst = createRescaleOp(image, slope, y_int);

            // produce a byte image
            pb = new ParameterBlock();
            pb.addSource(dst);
            pb.add(DataBuffer.TYPE_BYTE);
            dst = JAI.create("format", pb, null);
        }

        return dst;
    }


    /**
     * Retrieves the histogram (if any) of the given image.
     */
    public static Histogram getHistogramOf(PlanarImage sourceImage) {
        final Object property = sourceImage.getProperty("histogram");
        if (property instanceof Histogram) {
            return (Histogram) property;
        }
        return null;
    }

    /**
     * Returns an array containing the minimum and maximum value of the native data type used to store pixel values in
     * the given image.
     */
    public static double[] getNativeMinMaxOf(PlanarImage sourceImage, double[] minmax) {
        return ImageUtils.getDataTypeMinMax(sourceImage.getSampleModel().getDataType(), minmax);
    }

    /**
     * Creates an image with a histogram attached to it. The actual histogram instance can be optained via the
     * <code>getHistogramOf()</code> method.
     */
    public static RenderedOp createHistogramImage(PlanarImage sourceImage, int binCount) {
        double[] minmax = getNativeMinMaxOf(sourceImage, null);
        return createHistogramImage(sourceImage, binCount, minmax[0], minmax[1]);
    }

    /**
     * Creates an image with a histogram attached to it. The actual histogram instance can be optained via the
     * <code>getHistogramOf()</code> method.
     */
    public static RenderedOp createHistogramImage(PlanarImage sourceImage, int binCount, double minValue,
                                                  double maxValue) {

        int numBands = sourceImage.getSampleModel().getNumBands();

        // Allocate histogram memory.
        int[] numBins = new int[numBands];
        double[] lowValue = new double[numBands];
        double[] highValue = new double[numBands];
        for (int i = 0; i < numBands; i++) {
            numBins[i] = binCount;
            lowValue[i] = minValue;
            highValue[i] = maxValue;
        }

        // Create the Histogram object.
        Histogram histogram = new Histogram(numBins, lowValue, highValue);
        sourceImage.setProperty("histogram", histogram); // Specify the histogram

        // Create the parameter block.
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(sourceImage);    // Specify the source image
        pb.add(null); // roi
        pb.add(1);   // xPeriod
        pb.add(1);  // yPeriod
        pb.add(histogram.getNumBins()); // ?
        pb.add(histogram.getLowValue()); // ?
        pb.add(histogram.getHighValue()); // ?

        // Perform the histogram operation.
        return JAI.create("histogram", pb, null);
    }

    /**
     * Creates an equalization CDF image.
     */
    public static RenderedOp createHistogramEqualizedImage(PlanarImage sourceImage) {

        int numBands = sourceImage.getSampleModel().getNumBands();

        Histogram histogram = getHistogramOf(sourceImage);
        if (histogram == null) {
            sourceImage = createHistogramImage(sourceImage, 256);
            histogram = getHistogramOf(sourceImage);
        }

        // Create an equalization CDF.
        float[][] eqCDF = new float[numBands][];
        for (int b = 0; b < numBands; b++) {
            int binCount = histogram.getNumBins(b);
            eqCDF[b] = new float[binCount];
            for (int i = 0; i < binCount; i++) {
                eqCDF[b][i] = (float) (i + 1) / (float) binCount;
            }
        }

        // Create a histogram-equalized image.
        return JAI.create("matchcdf", sourceImage, eqCDF);
    }

    /**
     * Creates a normalization CDF image.
     * @param sourceImage the image to normalze
     * @return the normalized image
     */
    public static RenderedOp createHistogramNormalizedImage(PlanarImage sourceImage) {
        final double[] minmax = getNativeMinMaxOf(sourceImage, null);
        final double dev = minmax[1] - minmax[0];
        final double mean = minmax[0] + 0.5 * dev;
        final double stdDev = 0.25 * dev;
        int numBands = sourceImage.getSampleModel().getNumBands();
        final double[] means = new double[numBands];
        Arrays.fill(means, mean);
        final double[] stdDevs = new double[numBands];
        Arrays.fill(stdDevs, stdDev);
        return createHistogramNormalizedImage(sourceImage, means, stdDevs);
    }

    /**
     * Creates a normalization CDF image.
     * @param sourceImage The image to normalize
     * @param mean The mean values for each band of the image.
     * @param stdDev The standard deviation for each band of the image.
     * @return the normalized image
     */
    public static RenderedOp createHistogramNormalizedImage(PlanarImage sourceImage, double[] mean, double[] stdDev) {
        int numBands = sourceImage.getSampleModel().getNumBands();
        Assert.argument(numBands == mean.length, "length of mean must be equal to number of bands in the image");
        Assert.argument(numBands == stdDev.length, "length of stdDev must be equal to number of bands in the image");

        Histogram histogram = getHistogramOf(sourceImage);
        if (histogram == null) {
            sourceImage = createHistogramImage(sourceImage, 256);
            histogram = getHistogramOf(sourceImage);
        }

        float[][] normCDF = new float[numBands][];
        for (int b = 0; b < numBands; b++) {
            int binCount = histogram.getNumBins(b);
            normCDF[b] = new float[binCount];
            double mu = mean[b];
            double twoSigmaSquared = 2.0 * stdDev[b] * stdDev[b];
            normCDF[b][0] =
                    (float) Math.exp(-mu * mu / twoSigmaSquared);
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

        // Create a histogram-normalized image.
        return JAI.create("matchcdf", sourceImage, normCDF);
    }

    public static PlanarImage createAlphaOverlay(PlanarImage baseImage, PlanarImage alphaImage, Color color) {

        final PlanarImage sourcePIm2 = baseImage;
        final PlanarImage alphaPIm1 = alphaImage;

        final byte r = (byte) color.getRed();
        final byte g = (byte) color.getGreen();
        final byte b = (byte) color.getBlue();
        final int w = alphaPIm1.getWidth();
        final int h = alphaPIm1.getHeight();

        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setTileWidth(baseImage.getTileWidth());
        imageLayout.setTileHeight(baseImage.getTileHeight());
        final RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);

        ParameterBlock pb;
        PlanarImage sourcePIm1;
        if (sourcePIm2.getColorModel() instanceof IndexColorModel) {
            // For the first overlay, create a index color model image because:
            // 1. the base image (without any bitmask overlays) has an index color model
            // 2. the JAI composite operator requires the source images to have the same
            //    color models
            IndexColorModel cm = new IndexColorModel(8, 1, new byte[]{r}, new byte[]{g}, new byte[]{b});
            byte[] data = new byte[w * h]; // Zero filled --> zero index points to r,g,b
            BufferedImage sourceBIm1 = ImageUtils.createIndexedImage(w, h, data, cm);
            sourcePIm1 = PlanarImage.wrapRenderedImage(sourceBIm1);
        } else {
            // For all subsequential overlays, a constant image is sufficient (we don't know why!)
            Byte[] bandColors = new Byte[]{r, g, b};
            pb = new ParameterBlock();
            pb.add(new Float(w));
            pb.add(new Float(h));
            pb.add(bandColors);
            sourcePIm1 = JAI.create("constant", pb, renderingHints);
        }

        pb = new ParameterBlock();
        pb.add(new Float(w));  // The width
        pb.add(new Float(h));  // The height
        pb.add(new Byte[]{(byte) 255}); // The source alpha value
        final PlanarImage alphaPIm2 = JAI.create("constant", pb, renderingHints);

        pb = new ParameterBlock();
        pb.addSource(sourcePIm1);  // source 1 = constant bitmask-color image (overlay image)
        pb.addSource(sourcePIm2);  // source 2 = raster data image (base image)
        pb.add(alphaPIm1);         // alpha 1 = 255 * bitmask-alpha, if term(x,y) == true, else 0
        pb.add(alphaPIm2);         // alpha 2 = 255
        pb.add(Boolean.FALSE);
        pb.add(CompositeDescriptor.NO_DESTINATION_ALPHA);
        PlanarImage sourcePIm3 = JAI.create("composite", pb, renderingHints);

//        JAIDebug.trace("sourcePIm1", sourcePIm1, JAIDebug.F_ALL);
//        JAIDebug.trace("baseImage", baseImage, JAIDebug.F_ALL);
//        JAIDebug.trace("alphaImage", alphaImage, JAIDebug.F_ALL);
//        JAIDebug.trace("alphaPIm2", alphaPIm2, JAIDebug.F_ALL);
//        JAIDebug.trace("sourcePIm3", sourcePIm3, JAIDebug.F_ALL);

        return sourcePIm3;
    }

    public static LookupTableJAI createColorLookupTable(IndexColorModel icm) {
        int numBands = icm.hasAlpha() ? 4 : 3;
        byte[][] data = new byte[numBands][icm.getMapSize()];
        icm.getReds(data[0]);
        icm.getGreens(data[1]);
        icm.getBlues(data[2]);
        if (numBands == 4) {
            icm.getAlphas(data[3]);
        }
        return new LookupTableJAI(data);
    }

    public static Dimension computePreferredTileSize(int imageWidth,
                                                     int imageHeight,
                                                     int granularity) {
        return new Dimension(computePreferredTileSize(imageWidth, granularity),
                             computePreferredTileSize(imageHeight, granularity));
    }

    private static int computePreferredTileSize(int imageSize, int granularity) {
        if (imageSize <= MAX_TILE_SIZE) {
            return imageSize;
        }
        for (int u = MAX_TILE_SIZE; u >= MIN_TILE_SIZE; u -= TILE_SIZE_STEP) {
            if (imageSize % u == 0) {
                return u;
            }
        }
        int minDelta = Integer.MAX_VALUE;
        int tileSize = -1;
        for (int u = MAX_TILE_SIZE; u >= MIN_TILE_SIZE; u -= granularity) {
            int n = imageSize / u;
            if (n * u == imageSize) {
                return u;
            } else if (n * u < imageSize) {
                n++;
            }
            final int delta = Math.abs(n * u - imageSize);
            if (delta < minDelta) {
                minDelta = delta;
                tileSize = u;
            }
        }
        Assert.state(tileSize != -1);
        return tileSize;
    }

    public static PlanarImage createMapping2(RenderedImage sourceImage, IntMap indexMap) {
        final Raster sourceData = sourceImage.getData();
        final WritableRaster targetData = sourceData.createCompatibleWritableRaster();
        final DataBuffer targetBuffer = targetData.getDataBuffer();
        for (int i = 0; i < targetBuffer.getSize(); i++) {
            final int index = indexMap.getValue(sourceData.getDataBuffer().getElem(i));
            targetBuffer.setElem(i, index);
        }

        final BufferedImage image = new BufferedImage(sourceData.getWidth(),
                                                      sourceData.getHeight(),
                                                      BufferedImage.TYPE_BYTE_GRAY);
        image.setData(targetData);
        return PlanarImage.wrapRenderedImage(image);
    }

    public static PlanarImage createIndexedImage(RenderedImage sourceImage, IntMap intMap, int undefinedIndex) {
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
        sourceImage = ClampDescriptor.create(sourceImage, new double[]{keyMin - 1}, new double[]{keyMax + 1}, null);
        return LookupDescriptor.create(sourceImage, lookup, null);
    }


} // ImageUtils


