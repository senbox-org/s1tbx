package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.geotools.resources.XArray;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * @author Tonio Fincke
 */
class Aggregate {

    enum Type {FIRST, MIN, MAX, MEDIAN, MEAN}

    static MultiLevelImage createAggregatedMultiLevelImage(Band sourceBand, final RasterDataNode referenceNode, Type type) {
        final RenderingHints targetHints = getRenderingHints(sourceBand.getNoDataValue());
        final AffineTransform transform = new AffineTransform(sourceBand.getMultiLevelModel().getImageToModelTransform(0));
        transform.concatenate(referenceNode.getMultiLevelModel().getModelToImageTransform(0));
        Band band = sourceBand;
        final GeneralFilterBand.OpType opType = getOpType(type);
        if (opType != null) {
            int kernelWidth = (int) (1 / transform.getScaleX());
            int kernelHeight = (int) (1 / transform.getScaleY());
            final Kernel kernel = new Kernel(kernelWidth, kernelHeight, new double[kernelWidth * kernelHeight]);
            band = new GeneralFilterBand("filteredBand", sourceBand, opType, kernel, 1);
        }
        final Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        return AggregationScaler.scaleMultiLevelImage(referenceNode.getSourceImage(), band.getSourceImage(),
                                                      new float[]{(float) transform.getScaleX(), (float) transform.getScaleY()},
                                                      new float[]{(float) transform.getTranslateX(), (float) transform.getTranslateY()},
                                                      targetHints, sourceBand.getNoDataValue(), interpolation);
    }

    private static GeneralFilterBand.OpType getOpType(Type type) {
        switch (type) {
            case MIN:
                return GeneralFilterBand.OpType.MIN;
            case MAX:
                return GeneralFilterBand.OpType.MAX;
            case MEDIAN:
                return GeneralFilterBand.OpType.MEDIAN;
            case MEAN:
                return GeneralFilterBand.OpType.MEAN;
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
