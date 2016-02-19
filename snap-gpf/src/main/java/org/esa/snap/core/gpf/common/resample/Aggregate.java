package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.jai.GeneralFilterFunction;
import com.bc.ceres.jai.operator.GeneralFilterDescriptor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.FillConstantOpImage;
import org.geotools.resources.XArray;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.FormatDescriptor;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;

/**
 * @author Tonio Fincke
 */
class Aggregate {

    enum Type {FIRST, MIN, MAX, MEDIAN, MEAN}

    static MultiLevelImage createAggregatedMultiLevelImage(Band sourceBand, final RasterDataNode referenceNode,
                                                           Type type, Type flagAggregationMethod) {
        final RenderingHints targetHints = getRenderingHints(sourceBand.getNoDataValue());
        final AffineTransform sourceTransform = sourceBand.getMultiLevelModel().getImageToModelTransform(0);
        final AffineTransform transform = new AffineTransform(sourceTransform);
        transform.concatenate(referenceNode.getMultiLevelModel().getModelToImageTransform(0));
        final AffineTransform referenceTransform = referenceNode.getImageToModelTransform();
        float translateX = (float) (sourceTransform.getTranslateX() / sourceTransform.getScaleX()) -
                (float) (referenceTransform.getTranslateX() / sourceTransform.getScaleX());
        float translateY = (float) (sourceTransform.getTranslateY() / sourceTransform.getScaleY()) -
                (float) (referenceTransform.getTranslateY() / sourceTransform.getScaleY());
        int kernelWidth = (int) (1 / transform.getScaleX());
        int kernelHeight = (int) (1 / transform.getScaleY());
        final Kernel kernel = new Kernel(kernelWidth, kernelHeight, new double[kernelWidth * kernelHeight]);
        final GeneralFilterFunction filterFunction = getFilterFunction(type, kernel);
        final Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        MultiLevelImage image = sourceBand.getSourceImage();
        if (filterFunction != null) {
            if (sourceBand.isFlagBand()) {
                final RenderedOp formattedOp = FormatDescriptor.create(image, DataBuffer.TYPE_FLOAT, null);
                final FillConstantOpImage fillConstantOpImage = new FillConstantOpImage(formattedOp, null, Float.NaN);
                final RenderedOp filteredImage = GeneralFilterDescriptor.create(fillConstantOpImage,
                                                                                filterFunction, getRenderingHints(Double.NaN));
                image = new DefaultMultiLevelImage(new DefaultMultiLevelSource(filteredImage, sourceBand.getMultiLevelModel()));
            } else {
                final RenderedOp filteredImage = GeneralFilterDescriptor.create(sourceBand.getSourceImage(),
                                                                                filterFunction, getRenderingHints(Double.NaN));
                image = new DefaultMultiLevelImage(new DefaultMultiLevelSource(filteredImage, sourceBand.getMultiLevelModel()));
            }
        }
        return AggregationScaler.scaleMultiLevelImage(referenceNode.getSourceImage(), image,
                                                      new float[]{(float) transform.getScaleX(), (float) transform.getScaleY()},
                                                      new float[]{translateX, translateY},
                                                      targetHints,
                                                      sourceBand.getNoDataValue(),
                                                      interpolation);
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
            float largestFloat = Float.MIN_VALUE;
            for (float aFdata : fdata) {
                if (aFdata > largestFloat) {
                    largestFloat = aFdata;
                }
            }
            int n = 0;
            final int numberOfRelevantBits = (int) Math.floor(Math.sqrt(largestFloat)) + 1;
            final int[] occurenceCounter = new int[numberOfRelevantBits];
            for (int i = 0; i < fdata.length; i++) {
                if ((se == null || se[i]) && !Float.isNaN(fdata[i])) {
                    Long v = (long) fdata[i];
                    for (int j = 0; j < numberOfRelevantBits; j++) {
                        long compare = v & 1;
                        if (compare != 0) {
                            occurenceCounter[j]++;
                        }
                        v >>= 1;
                    }
                    n++;
                }
            }
            long res = 0;
            for (int i = numberOfRelevantBits - 1; i >= 0; i--) {
                res <<= 1;
                if (occurenceCounter[i] > (n / 2)) {
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
            float largestFloat = Float.MIN_VALUE;
            for (float aFdata : fdata) {
                if (aFdata > largestFloat) {
                    largestFloat = aFdata;
                }
            }
            int n = 0;
            final int numberOfRelevantBits = (int) Math.floor(Math.sqrt(largestFloat));
            final int[] occurenceCounter = new int[numberOfRelevantBits];
            for (int i = 0; i < fdata.length; i++) {
                if ((se == null || se[i]) && !Float.isNaN(fdata[i])) {
                    Long v = (long) fdata[i];
                    for (int j = 0; j < numberOfRelevantBits; j++) {
                        long compare = v & 1;
                        if (compare != 0) {
                            occurenceCounter[j]++;
                        }
                        v >>= 1;
                    }
                    n++;
                }
            }
            long res = 0;
            for (int i = 0; i < numberOfRelevantBits; i++) {
                if (occurenceCounter[i] >= (n / 2)) {
                    res++;
                }
                res <<= 1;
            }
            return (float) res;
        }

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

}
