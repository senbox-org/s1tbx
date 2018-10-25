package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.apache.commons.math3.util.Precision;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ResolutionLevel;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

/**
 * @author Tonio Fincke
 */
class InterpolationScaler {

    static MultiLevelImage scaleMultiLevelImage(int masterWidth, int masterHeight, Dimension tileSize,
                                                       MultiLevelModel masterMultiLevelModel, MultiLevelImage sourceImage,
                                                       float[] scalings, RenderingHints renderingHints,
                                                       double noDataValue, Interpolation interpolation) {
        final ScaledMultiLevelSource multiLevelSource = new ScaledMultiLevelSource(masterWidth, masterHeight,
                                                                                   tileSize,
                                                                                   masterMultiLevelModel,
                                                                                   sourceImage,
                                                                                   scalings, renderingHints,
                                                                                   noDataValue, interpolation);
        return new DefaultMultiLevelImage(multiLevelSource);
    }

    private static class ScaledMultiLevelSource extends AbstractMultiLevelSource {

        private final MultiLevelImage sourceImage;
        private final float[] scalings;
        private final RenderingHints renderingHints;
        private final double noDataValue;
        private final MultiLevelModel masterMultiLevelModel;
        private final int masterWidth;
        private final int masterHeight;
        private Interpolation interpolation;

        private static double EPSILON = 1E-12;
        private final Dimension tileSize;

        private ScaledMultiLevelSource(int masterWidth, int masterHeight, Dimension tileSize,
                                       MultiLevelModel masterMultiLevelModel,
                                       MultiLevelImage sourceImage, float[] scalings, RenderingHints renderingHints,
                                       double noDataValue, Interpolation interpolation) {
            super(new DefaultMultiLevelModel(masterMultiLevelModel.getLevelCount(), new AffineTransform(),
                                             masterWidth, masterHeight));
            this.tileSize = tileSize;
            this.sourceImage = sourceImage;
            this.scalings = scalings;
            this.renderingHints = renderingHints;
            this.noDataValue = noDataValue;
            this.interpolation = interpolation;
            this.masterWidth = masterWidth;
            this.masterHeight = masterHeight;
            this.masterMultiLevelModel = masterMultiLevelModel;
        }

        @Override
        protected RenderedImage createImage(int targetLevel) {
            final double targetScale = masterMultiLevelModel.getScale(targetLevel);
            final ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), targetLevel);
            final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(sourceImage.getSampleModel().getDataType(),
                                                                                       null,
                                                                                       masterWidth,
                                                                                       masterHeight,
                                                                                       tileSize,
                                                                                       resolutionLevel);
            final int levelMasterWidth = imageLayout.getWidth(null);
            final int levelMasterHeight = imageLayout.getHeight(null);
            final MultiLevelModel sourceModel = sourceImage.getModel();
            final int sourceLevel = findBestSourceLevel(targetScale, sourceModel, scalings);
            final double sourceScale = sourceModel.getScale(sourceLevel);
            final RenderedImage image = sourceImage.getImage(sourceLevel);
            final float scaleRatio = (float) (sourceScale / targetScale);
            RenderedImage renderedImage = image;
            final float xScale = scalings[0] * scaleRatio;
            final float yScale = scalings[1] * scaleRatio;
            final AffineTransform sourceTransform = sourceModel.getImageToModelTransform(sourceLevel);
            final AffineTransform referenceTransform = masterMultiLevelModel.getImageToModelTransform(targetLevel);
            float offsetX = (float) (sourceTransform.getTranslateX() / sourceTransform.getScaleX()) -
                    (float) (referenceTransform.getTranslateX() / sourceTransform.getScaleX());
            float offsetY = (float) (sourceTransform.getTranslateY() / sourceTransform.getScaleY()) -
                    (float) (referenceTransform.getTranslateY() / sourceTransform.getScaleY());
            if (Precision.compareTo((double) offsetX, 0.0, EPSILON) != 0 ||
                    Precision.compareTo((double) offsetY, 0.0, EPSILON) != 0) {
                renderedImage = TranslateDescriptor.create(renderedImage, offsetX, offsetY, null,
                                                           renderingHints);
            }
            if (Precision.compareTo((double) xScale, 1.0, EPSILON) != 0
                    || Precision.compareTo((double) yScale, 1.0, EPSILON) != 0) {
                renderedImage = ScaleDescriptor.create(renderedImage, xScale, yScale, 0.5f, 0.5f, interpolation, renderingHints);
            }
            if (levelMasterWidth != renderedImage.getWidth() || levelMasterHeight != renderedImage.getHeight() ||
                    Precision.compareTo((double) offsetX, 0.0, EPSILON) != 0 ||
                    Precision.compareTo((double) offsetY, 0.0, EPSILON) != 0) {
                final int leftPad = renderedImage.getMinX();
                final int upperPad = renderedImage.getMinY();
                final BorderExtender borderExtender = new BorderExtenderConstant(new double[]{noDataValue});
                final int rightPad = Math.max(0, levelMasterWidth - leftPad - renderedImage.getWidth());
                final int lowerPad = Math.max(0, levelMasterHeight - upperPad - renderedImage.getHeight());
                renderedImage = BorderDescriptor.create(renderedImage,
                                                        leftPad,
                                                        rightPad,
                                                        upperPad,
                                                        lowerPad,
                                                        borderExtender, renderingHints);
            }
            renderedImage = CropDescriptor.create(renderedImage, 0.0f, 0.0f, (float) levelMasterWidth, (float) levelMasterHeight,
                                                  renderingHints);
            return renderedImage;
        }

        //todo remove code duplication with sourceimagescaler and resamplingop - tf 20160314
        private int findBestSourceLevel(double targetScale, MultiLevelModel sourceModel, float[] scalings) {
            /*
             * Find the source level such that the final scaling factor is the closest to 1.0
             *
             * Example : When scaling a 20m resolution image to 10m resolution,
             * when generating the level 1 image of the scaled image, we prefer using the source image data at level 0,
             * since it will provide a better resolution than upscaling by 2 the source image data at level 1.
             *
             * We can't find the best on both X and Y directions if scaling factors are arbitrary, so we limit the
             * search algorithm by optimizing only for the X direction.
             * This will cover the most frequent use case where scaling factors in both directions are equal.
             */
            float optimizedScaling = 0;
            int optimizedSourceLevel = 0;
            boolean initialized = false;
            for (int sourceLevel = 0; sourceLevel < sourceModel.getLevelCount(); sourceLevel++) {
                final double sourceScale = sourceModel.getScale(sourceLevel);
                final float scaleRatio = (float) (sourceScale / targetScale);
                if (!initialized) {
                    optimizedScaling = scalings[0] * scaleRatio;
                    optimizedSourceLevel = sourceLevel;
                    initialized = true;
                } else {
                    // We want to be as close to 1.0 as possible
                    if (Math.abs(1 - scalings[0] * scaleRatio) < Math.abs(1 - optimizedScaling)) {
                        optimizedScaling = scalings[0] * scaleRatio;
                        optimizedSourceLevel = sourceLevel;
                    }
                }
            }
            return optimizedSourceLevel;
        }

    }

}
