/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf.features.local.matching;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.Tile;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.analysis.algorithm.TemplateMatcher;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

/**
 *
 * @author Emanuela
 */
public class SARTemplateMatcher implements TemplateMatcherEnforcement {

    FImage image;
    FImage template;
    Rectangle srcTileRectangle;

    @Override
    public float computeMatchScore(Band sourceBand, Tile templateTile, int x, int y,
            Mode mode) {

        final RenderedImage fullRenderedImage = sourceBand.getSourceImage().getImage(0);
        final BufferedImage fullBufferedImage =
                new BufferedImage(sourceBand.getSceneRasterWidth(),
                sourceBand.getSceneRasterHeight(),
                BufferedImage.TYPE_USHORT_GRAY);
        fullBufferedImage.setData(fullRenderedImage.getData());
        image = ImageUtilities.createFImage(fullBufferedImage);

        srcTileRectangle = templateTile.getRectangle();

        BufferedImage bf = fullBufferedImage.getSubimage(srcTileRectangle.x, srcTileRectangle.y,
                srcTileRectangle.width, srcTileRectangle.height);
        template = ImageUtilities.createFImage(bf);
        final float[][] imageData = image.pixels;
        final float[][] templateData = template.pixels;
        float fNorm;
        switch (mode) {
            case SUM_SQUARED_DIFFERENCE:
                fNorm = TemplateMatcher.Mode.SUM_SQUARED_DIFFERENCE.computeMatchScore(imageData, x, y,
                        templateData, srcTileRectangle.x, srcTileRectangle.y, srcTileRectangle.width,
                        srcTileRectangle.height);
                break;
            case NORM_SUM_SQUARED_DIFFERENCE:
                fNorm = TemplateMatcher.Mode.NORM_SUM_SQUARED_DIFFERENCE.computeMatchScore(imageData, x, y,
                        templateData, srcTileRectangle.x, srcTileRectangle.y, srcTileRectangle.width,
                        srcTileRectangle.height);
                break;
            case CORRELATION:
                fNorm = TemplateMatcher.Mode.CORRELATION.computeMatchScore(imageData, x, y,
                        templateData, srcTileRectangle.x, srcTileRectangle.y, srcTileRectangle.width,
                        srcTileRectangle.height);
                break;
            case NORM_CORRELATION:
                fNorm = TemplateMatcher.Mode.NORM_CORRELATION.computeMatchScore(imageData, x, y,
                        templateData, srcTileRectangle.x, srcTileRectangle.y, srcTileRectangle.width,
                        srcTileRectangle.height);
                break;
            case CORRELATION_COEFFICIENT:
                fNorm = TemplateMatcher.Mode.CORRELATION_COEFFICIENT.computeMatchScore(imageData, x, y,
                        templateData, srcTileRectangle.x, srcTileRectangle.y, srcTileRectangle.width,
                        srcTileRectangle.height);
                break;
            case NORM_CORRELATION_COEFFICIENT:
                fNorm = TemplateMatcher.Mode.CORRELATION_COEFFICIENT.computeMatchScore(imageData, x, y,
                        templateData, srcTileRectangle.x, srcTileRectangle.y, srcTileRectangle.width,
                        srcTileRectangle.height);
                break;
            default:
                fNorm = 0f;
        }
        return fNorm;
    }

    /**
     * Compute the score at a point as the sum-squared difference between the
     * image and the template with the top-left at the given point. The
     * SARTemplateMatcher will account for the offset to the centre of the
     * template internally.
     *
     */
    public enum Mode {

        SUM_SQUARED_DIFFERENCE, NORM_SUM_SQUARED_DIFFERENCE, CORRELATION,
        NORM_CORRELATION, CORRELATION_COEFFICIENT, NORM_CORRELATION_COEFFICIENT
    }
}