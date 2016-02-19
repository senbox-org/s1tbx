package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.SourceImageScaler;
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
class Interpolate {

    static MultiLevelImage createInterpolatedMultiLevelImage(Band sourceBand, final RasterDataNode referenceNode, Interpolation interpolation) {
        final MultiLevelModel referenceModel = referenceNode.getMultiLevelModel();
        final AffineTransform transform = new AffineTransform(sourceBand.getImageToModelTransform());
        transform.concatenate(referenceModel.getModelToImageTransform(0));
        final RenderingHints targetHints = getRenderingHints(sourceBand.getNoDataValue());
        return SourceImageScaler.scaleMultiLevelImage(referenceNode.getSourceImage(), sourceBand.getSourceImage(),
                                                      new float[]{(float) transform.getScaleX(), (float) transform.getScaleY()},
                                                      new float[]{(float) transform.getTranslateX(), (float) transform.getTranslateY()},
                                                      targetHints, sourceBand.getNoDataValue(), interpolation);
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
