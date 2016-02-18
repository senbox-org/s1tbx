package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.bc.ceres.jai.GeneralFilterFunction;
import com.bc.ceres.jai.operator.GeneralFilterDescriptor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.geotools.resources.XArray;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * @author Tonio Fincke
 */
class Aggregate {

    enum Type {FIRST, MIN, MAX, MEDIAN, MEAN}

    static MultiLevelImage createAggregatedMultiLevelImage(Band sourceBand, final RasterDataNode referenceNode, Type type) {
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
            final RenderedOp filteredImage = GeneralFilterDescriptor.create(sourceBand.getSourceImage(),
                                                                            filterFunction, getRenderingHints(Double.NaN));
            image = new DefaultMultiLevelImage(new DefaultMultiLevelSource(filteredImage, sourceBand.getMultiLevelModel()));
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
