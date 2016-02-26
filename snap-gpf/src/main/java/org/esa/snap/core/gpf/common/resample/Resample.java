package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.jai.GeneralFilterFunction;
import com.bc.ceres.jai.operator.GeneralFilterDescriptor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.geotools.resources.XArray;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.operator.FormatDescriptor;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

/**
 * @author Tonio Fincke
 */
class Resample {

    enum Type {FIRST, MIN, MAX, MEDIAN, MEAN, MIN_MEDIAN, MAX_MEDIAN}

    static MultiLevelImage createInterpolatedMultiLevelImage(Band sourceBand, final RasterDataNode referenceNode, Interpolation interpolation) {
        final RenderingHints targetHints = getRenderingHints(sourceBand.getNoDataValue());
        final float[] scalings = getScalings(sourceBand, referenceNode);
        return ResamplingScaler.scaleMultiLevelImage(referenceNode.getSourceImage(), sourceBand.getSourceImage(),
                                                     scalings, targetHints, sourceBand.getNoDataValue(), interpolation);
    }

    static MultiLevelImage createAggregatedMultiLevelImage(Band sourceBand, final RasterDataNode referenceNode,
                                                           Type type, Type flagType) {
        final RenderingHints targetHints = getRenderingHints(sourceBand.getNoDataValue());
        final float[] scalings = getScalings(sourceBand, referenceNode);
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
        if (filterFunction != null) {
            RenderedImage image = sourceBand.getSourceImage();
            if (!ProductData.isFloatingPointType(sourceBand.getDataType())) {
                image = FormatDescriptor.create(image, DataBuffer.TYPE_FLOAT, null);
            }
            image = GeneralFilterDescriptor.create(image, filterFunction, getRenderingHints(Double.NaN));
            if (!ProductData.isFloatingPointType(sourceBand.getDataType())) {
                image = FormatDescriptor.create(image, getDataBufferType(sourceBand), null);
            }
            multiLevelImage = new DefaultMultiLevelImage(new DefaultMultiLevelSource(image, sourceBand.getMultiLevelModel()));
        }
        return ResamplingScaler.scaleMultiLevelImage(referenceNode.getSourceImage(), multiLevelImage, scalings,
                                                     targetHints, sourceBand.getNoDataValue(), interpolation);
    }

    private static float[] getScalings(Band sourceBand, RasterDataNode referenceNode) {
        final AffineTransform sourceTransform = sourceBand.getMultiLevelModel().getImageToModelTransform(0);
        final AffineTransform transform = new AffineTransform(sourceTransform);
        transform.concatenate(referenceNode.getMultiLevelModel().getModelToImageTransform(0));
        return new float[]{(float) transform.getScaleX(), (float) transform.getScaleY()};
    }

    private static int getDataBufferType(Band sourceBand) {
        switch(sourceBand.getDataType()) {
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
        }
        return DataBuffer.TYPE_UNDEFINED;
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

}
