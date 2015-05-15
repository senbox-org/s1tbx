package org.esa.snap.jai;

/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.Interpolation;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

public class SourceImageScaler {

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
            if (xScale != 1.0f || yScale != 1.0f) {
                renderedImage = ScaleDescriptor.create(image, xScale, yScale, 0.5f, 0.5f, interpolation, renderingHints);
            }
            final float scaledXOffset = (offsets != null) ? (float) (offsets[0] / targetScale) : 0f;
            final float scaledYOffset = (offsets != null) ? (float) (offsets[1] / targetScale) : 0f;
            if (masterWidth != renderedImage.getWidth() || masterHeight != renderedImage.getHeight() ||
                    scaledXOffset != 0.0f || scaledYOffset != 0.0f) {
                final int padX = Math.round(scaledXOffset);
                final int padY = Math.round(scaledYOffset);
                int borderCorrectorX = (scaledXOffset - padX < 0) ? 1 : 0;
                int borderCorrectorY = (scaledYOffset - padY < 0) ? 1 : 0;
                final BorderExtender borderExtender = new BorderExtenderConstant(new double[]{noDataValue});
                //todo maybe remove Math.max when useful test data for SLSTR L1B products has arrived
                final int rightPadX = Math.max(0, masterWidth - padX - renderedImage.getWidth() + borderCorrectorX);
                final int lowerPadY = Math.max(0, masterHeight - padY - renderedImage.getHeight() + borderCorrectorY);
                renderedImage = BorderDescriptor.create(renderedImage,
                                                        padX,
                                                        rightPadX,
                                                        padY,
                                                        lowerPadY,
                                                        borderExtender, renderingHints);
            }
            if (scaledXOffset != 0.0f || scaledYOffset != 0.0f) {
                renderedImage = TranslateDescriptor.create(renderedImage, scaledXOffset, scaledYOffset, null,
                                                           renderingHints);
            }
            renderedImage = CropDescriptor.create(renderedImage, 0.0f, 0.0f, (float) masterWidth, (float) masterHeight,
                                                  renderingHints);
            return renderedImage;
        }

    }

}
