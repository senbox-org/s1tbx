package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.jai.GeneralFilterFunction;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.geotools.resources.XArray;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.GeometricOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.util.Map;
import java.util.Vector;

/**
 * @author Tonio Fincke
 */
public class Resample {

    enum Type {FIRST, MIN, MAX, MEDIAN, MEAN, MIN_MEDIAN, MAX_MEDIAN}

    public static MultiLevelImage createInterpolatedMultiLevelImage(MultiLevelImage sourceImage, double noDataValue,
                                                             AffineTransform sourceImageToModelTransform,
                                                             final int referenceWidth, int referenceHeight,
                                                             Dimension tileSize,
                                                             MultiLevelModel referenceMultiLevelModel,
                                                             Interpolation interpolation) {
        final RenderingHints targetHints = getRenderingHints(noDataValue);
        final float[] scalings = getScalings(sourceImageToModelTransform, referenceMultiLevelModel);
        return InterpolationScaler.scaleMultiLevelImage(referenceWidth, referenceHeight, tileSize,
                                                        referenceMultiLevelModel, sourceImage,
                                                        scalings, targetHints, noDataValue, interpolation);
    }

    private static float[] getScalings(AffineTransform sourceTransform, MultiLevelModel referenceMultiLevelModel) {
        final AffineTransform transform = new AffineTransform(sourceTransform);
        transform.concatenate(referenceMultiLevelModel.getModelToImageTransform(0));
        return new float[]{(float) transform.getScaleX(), (float) transform.getScaleY()};
    }

    static MultiLevelImage createInterpolatedMultiLevelImage(Band sourceBand, final RasterDataNode referenceNode, Interpolation interpolation) {
        final RenderingHints targetHints = getRenderingHints(sourceBand.getNoDataValue());
        final float[] scalings = getScalings(sourceBand.getImageToModelTransform(), referenceNode);
        return ResamplingScaler.scaleMultiLevelImage(referenceNode.getSourceImage(), sourceBand.getSourceImage(),
                                                     scalings, null, targetHints, sourceBand.getNoDataValue(), interpolation);
    }

    static MultiLevelImage createAggregatedMultiLevelImage(Band sourceBand, final RasterDataNode referenceNode,
                                                           Type type, Type flagType) {
        final RenderingHints targetHints = getRenderingHints(sourceBand.getNoDataValue());
        final float[] scalings = getScalings(sourceBand.getImageToModelTransform(), referenceNode);
        int kernelWidth = (int) (1 / scalings[0]);
        int kernelHeight = (int) (1 / scalings[1]);
        final Kernel kernel = new Kernel(kernelWidth, kernelHeight, new double[kernelWidth * kernelHeight]);
        final GeneralFilterFunction filterFunction;
        if (sourceBand.isFlagBand() || sourceBand.isIndexBand()) {
            filterFunction = getFlagFilterFunction(flagType, kernel);
        } else {
            filterFunction = getFilterFunction(type, kernel);
        }
        final Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        MultiLevelImage multiLevelImage = sourceBand.getSourceImage();
        return ResamplingScaler.scaleMultiLevelImage(referenceNode.getSourceImage(), multiLevelImage, scalings, filterFunction,
                                                     targetHints, sourceBand.getNoDataValue(), interpolation);
    }

    private static float[] getScalings(AffineTransform sourceTransform, RasterDataNode referenceNode) {
        final AffineTransform transform = new AffineTransform(sourceTransform);
        transform.concatenate(referenceNode.getMultiLevelModel().getModelToImageTransform(0));
        return new float[]{(float) transform.getScaleX(), (float) transform.getScaleY()};
    }

    private static RenderingHints getRenderingHints(double noDataValue) {
        RenderingHints hints = new RenderingHints(null);
        final double[] background = new double[]{noDataValue};
        final BorderExtender borderExtender;
        if (XArray.allEquals(background, 0)) {
            borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);
        } else {
            borderExtender = new BorderExtenderConstant(background);
        }
        hints.put(JAI.KEY_BORDER_EXTENDER, borderExtender);
        return hints;
    }

    private static GeneralFilterFunction getFilterFunction(Type type, Kernel kernel) {
        switch (type) {
            case MIN:
                return new GeneralFilterFunction.Min(
                        kernel.getWidth(), kernel.getHeight(), kernel.getXOrigin(), kernel.getYOrigin(), null);
            case MAX:
                return new GeneralFilterFunction.Max(
                        kernel.getWidth(), kernel.getHeight(), kernel.getXOrigin(), kernel.getYOrigin(), null);
            case MEDIAN:
                return new GeneralFilterFunction.Median(
                        kernel.getWidth(), kernel.getHeight(), kernel.getXOrigin(), kernel.getYOrigin(), null);
            case MEAN:
                return new GeneralFilterFunction.Mean(
                        kernel.getWidth(), kernel.getHeight(), kernel.getXOrigin(), kernel.getYOrigin(), null);
        }
        return null;
    }

    private static GeneralFilterFunction getFlagFilterFunction(Type type, Kernel kernel) {
        switch (type) {
            case MIN:
                return new FlagMinFunction(
                        kernel.getWidth(), kernel.getHeight(), kernel.getXOrigin(), kernel.getYOrigin(), null);
            case MAX:
                return new FlagMaxFunction(
                        kernel.getWidth(), kernel.getHeight(), kernel.getXOrigin(), kernel.getYOrigin(), null);
            case MIN_MEDIAN:
                return new FlagMedianMinFunction(
                        kernel.getWidth(), kernel.getHeight(), kernel.getXOrigin(), kernel.getYOrigin(), null);
            case MAX_MEDIAN:
                return new FlagMedianMaxFunction(
                        kernel.getWidth(), kernel.getHeight(), kernel.getXOrigin(), kernel.getYOrigin(), null);
        }
        return null;
    }

    static class FlagMinFunction extends GeneralFilterFunction {

        /**
         * Constructs a GeneralFilterFunction.
         *
         * @param width              the width of the kernel.
         * @param height             the height of the kernel.
         * @param xOrigin            the X coordinate of the key kernel element.
         * @param yOrigin            the Y coordinate of the key kernel element.
         * @param structuringElement The structuring element with a length equal to {@code width * height}. May be {@code null}.
         */
        protected FlagMinFunction(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        @Override
        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            long res = Long.MAX_VALUE;
            int n = 0;
            for (int i = 0; i < fdata.length; i++) {
                if ((se == null || se[i]) && !Float.isNaN(fdata[i])) {
                    long v = (long) fdata[i];
                    res = res & v;
                    n++;
                }
            }
            return n > 0 ? (float) res : 0;
        }

    }

    static class FlagMaxFunction extends GeneralFilterFunction {

        /**
         * Constructs a GeneralFilterFunction.
         *
         * @param width              the width of the kernel.
         * @param height             the height of the kernel.
         * @param xOrigin            the X coordinate of the key kernel element.
         * @param yOrigin            the Y coordinate of the key kernel element.
         * @param structuringElement The structuring element with a length equal to {@code width * height}. May be {@code null}.
         */
        protected FlagMaxFunction(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        @Override
        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            long res = 0;
            int n = 0;
            for (int i = 0; i < fdata.length; i++) {
                if ((se == null || se[i]) && !Float.isNaN(fdata[i])) {
                    long v = (long) fdata[i];
                    res = res | v;
                    n++;
                }
            }
            return n > 0 ? (float) res : 0;
        }

    }

    static class FlagMedianMinFunction extends GeneralFilterFunction {

        /**
         * Constructs a GeneralFilterFunction.
         *
         * @param width              the width of the kernel.
         * @param height             the height of the kernel.
         * @param xOrigin            the X coordinate of the key kernel element.
         * @param yOrigin            the Y coordinate of the key kernel element.
         * @param structuringElement The structuring element with a length equal to {@code width * height}. May be {@code null}.
         */
        protected FlagMedianMinFunction(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        @Override
        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            int n = 0;
            final int[] occurenceCounter = new int[63];
            int highestOccurence = 0;
            for (int i = 0; i < fdata.length; i++) {
                if ((se == null || se[i]) && !Float.isNaN(fdata[i])) {
                    Long v = (long) fdata[i];
                    int j = 0;
                    while (v > 0) {
                        long compare = v & 1;
                        if (compare != 0) {
                            occurenceCounter[j]++;
                        }
                        v >>= 1;
                        j++;
                    }
                    highestOccurence = Math.max(highestOccurence, j - 1);
                    n++;
                }
            }
            long res = 0;
            final float halfN = n / 2f;
            for (int i = highestOccurence; i >= 0; i--) {
                res <<= 1;
                if (occurenceCounter[i] > halfN) {
                    res++;
                }
            }
            return (float) res;
        }

    }

    static class FlagMedianMaxFunction extends GeneralFilterFunction {

        /**
         * Constructs a GeneralFilterFunction.
         *
         * @param width              the width of the kernel.
         * @param height             the height of the kernel.
         * @param xOrigin            the X coordinate of the key kernel element.
         * @param yOrigin            the Y coordinate of the key kernel element.
         * @param structuringElement The structuring element with a length equal to {@code width * height}. May be {@code null}.
         */
        protected FlagMedianMaxFunction(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        @Override
        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            int n = 0;
            final int[] occurenceCounter = new int[63];
            int highestOccurence = 0;
            for (int i = 0; i < fdata.length; i++) {
                if ((se == null || se[i]) && !Float.isNaN(fdata[i])) {
                    Long v = (long) fdata[i];
                    int j = 0;
                    while (v > 0) {
                        long compare = v & 1;
                        if (compare != 0) {
                            occurenceCounter[j]++;
                        }
                        v >>= 1;
                        j++;
                    }
                    highestOccurence = Math.max(highestOccurence, j - 1);
                    n++;
                }
            }
            long res = 0;
            final float halfN = n / 2f;
            for (int i = highestOccurence; i >= 0; i--) {
                res <<= 1;
                if (occurenceCounter[i] >= halfN) {
                    res++;
                }
            }
            return (float) res;
        }

    }

    private class NewImage extends PlanarImage {


        @Override
        public Raster getTile(int i, int i1) {
            return null;
        }
    }

    private class NewOp extends GeometricOpImage {


        public NewOp(Vector sources, ImageLayout layout, Map configuration, boolean cobbleSources, BorderExtender extender, Interpolation interp) {
            super(sources, layout, configuration, cobbleSources, extender, interp);
        }

        @Override
        protected Rectangle forwardMapRect(Rectangle rectangle, int i) {
            return null;
        }

        @Override
        protected Rectangle backwardMapRect(Rectangle rectangle, int i) {
            return null;
        }
    }

}
