package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.apache.commons.math3.util.Precision;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.Interpolation;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

/**
 * @author Tonio Fincke
 */
public class AggregationScaler {

    public static MultiLevelImage scaleMultiLevelImage(MultiLevelImage masterImage, MultiLevelImage sourceImage,
                                                       float[] scalings, float[] offsets,
                                                       RenderingHints renderingHints, double noDataValue,
                                                       Interpolation interpolation) {

        final ScaledMultiLevelSource multiLevelSource = new ScaledMultiLevelSource(masterImage, sourceImage,
                                                                                   scalings, offsets, renderingHints,
                                                                                   noDataValue,
                                                                                   interpolation);
        return new DefaultMultiLevelImage(multiLevelSource);
    }

    private static class ScaledMultiLevelSource extends AbstractMultiLevelSource {

        private final MultiLevelImage sourceImage;
        private final float[] scalings;
        private final RenderingHints renderingHints;
        private final double noDataValue;
        private final float[] offsets;
        private final MultiLevelImage masterImage;
        private Interpolation interpolation;

        private static double EPSILON = 1E-12;

        private ScaledMultiLevelSource(MultiLevelImage masterImage, MultiLevelImage sourceImage, float[] scalings,
                                       float[] offsets, RenderingHints renderingHints, double noDataValue,
                                       Interpolation interpolation) {
            super(masterImage.getModel());
            this.masterImage = masterImage;
            this.sourceImage = sourceImage;
            this.scalings = scalings;
            this.renderingHints = renderingHints;
            this.noDataValue = noDataValue;
            this.offsets = offsets;
            this.interpolation = interpolation;
        }

        @Override
        protected RenderedImage createImage(int targetLevel) {
            final int masterWidth = masterImage.getImage(targetLevel).getWidth();
            final int masterHeight = masterImage.getImage(targetLevel).getHeight();
            final MultiLevelModel sourceModel = sourceImage.getModel();
            final MultiLevelModel targetModel = getModel();
            final double targetScale = targetModel.getScale(targetLevel);
            final int sourceLevel = sourceModel.getLevel(targetScale);
            final double sourceScale = sourceModel.getScale(sourceLevel);
            final RenderedImage image = sourceImage.getImage(sourceLevel);
            final float scaleRatio = (float) (sourceScale / targetScale);
            RenderedImage renderedImage = image;
            final float xScale = scalings[0] * scaleRatio;
            final float yScale = scalings[1] * scaleRatio;
            if (Precision.compareTo((double) offsets[0], 0.0, EPSILON) != 0 ||
                    Precision.compareTo((double) offsets[1], 0.0, EPSILON) != 0) {
                renderedImage = TranslateDescriptor.create(renderedImage, offsets[0], offsets[1], null,
                                                           renderingHints);
            }
            if (Precision.compareTo((double) xScale, 1.0, EPSILON) != 0
                    || Precision.compareTo((double) yScale, 1.0, EPSILON) != 0) {
                renderedImage = ScaleDescriptor.create(renderedImage, xScale, yScale, 0.5f, 0.5f, interpolation, renderingHints);
            }
            if (masterWidth != renderedImage.getWidth() || masterHeight != renderedImage.getHeight() ||
                    Precision.compareTo((double) offsets[0], 0.0, EPSILON) != 0 ||
                    Precision.compareTo((double) offsets[1], 0.0, EPSILON) != 0) {
                final float scaledXOffset = offsets[0] * xScale;
                final float scaledYOffset = offsets[1] * yScale;
                final int leftPad = Math.round(scaledXOffset);
                final int upperPad = Math.round(scaledYOffset);
                int borderCorrectorX = (scaledXOffset - leftPad < 0) ? 1 : 0;
                int borderCorrectorY = (scaledYOffset - upperPad < 0) ? 1 : 0;
                final BorderExtender borderExtender = new BorderExtenderConstant(new double[]{noDataValue});
                final int rightPad = Math.max(0, masterWidth - leftPad - renderedImage.getWidth() + borderCorrectorX);
                final int lowerPad = Math.max(0, masterHeight - upperPad - renderedImage.getHeight() + borderCorrectorY);
                renderedImage = BorderDescriptor.create(renderedImage,
                                                        leftPad,
                                                        rightPad,
                                                        upperPad,
                                                        lowerPad,
                                                        borderExtender, renderingHints);
            }
            renderedImage = CropDescriptor.create(renderedImage, 0.0f, 0.0f, (float) masterWidth, (float) masterHeight,
                                                  renderingHints);
            return renderedImage;
        }
    }

}
