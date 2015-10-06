/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.image.LevelImageSupport;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.jexp.EvalEnv;

/**
 * Represents an evaluation environment for {@link org.esa.snap.core.jexp.Term Terms} which are operating on raster data.
 * <p>The evaluation environment is passed to the {@link org.esa.snap.core.jexp.Term#evalB(org.esa.snap.core.jexp.EvalEnv) evalB},
 * {@link org.esa.snap.core.jexp.Term#evalI(org.esa.snap.core.jexp.EvalEnv) evalI} and
 * {@link org.esa.snap.core.jexp.Term#evalB(org.esa.snap.core.jexp.EvalEnv) evalB} methods
 * of a {@link org.esa.snap.core.jexp.Term Term}.
 * <p>Special implementations of the {@link org.esa.snap.core.jexp.Symbol Symbol} and {@link org.esa.snap.core.jexp.Function Function}
 * interfaces, such as {@link RasterDataSymbol}, can then use the environment in order to perform
 * raster data specific evaluations.
 */
public class RasterDataEvalEnv implements EvalEnv {

    private final int offsetX;
    private final int offsetY;
    private final int regionWidth;
    private final int regionHeight;
    private int elemIndex;
    private LevelImageSupport levelImageSupport;

    /**
     * Constructs a new environment for the given raster data region.
     * Should only be used if evaluation takes place at image level zero.
     *
     * @param offsetX      the x-offset of the raster region
     * @param offsetY      the y-offset of the raster region
     * @param regionWidth  the width of the raster region
     * @param regionHeight the height of the raster region
     */
    public RasterDataEvalEnv(int offsetX, int offsetY, int regionWidth, int regionHeight) {
        this(offsetX, offsetY, regionWidth, regionHeight,
             new LevelImageSupport(regionWidth, regionHeight, ResolutionLevel.MAXRES));
    }

    /**
     * Constructs a new environment for the given raster data region.
     * Instances created with this constructor consider that the current data evaluation takes place
     * at a higher image level. The methods <code>getSourceX()</code> and <code>getSourceY()</code>
     * will return the correct pixel coordinate at level zero.
     *
     * @param offsetX           the x-offset of the raster region
     * @param offsetY           the y-offset of the raster region
     * @param regionWidth       the width of the raster region
     * @param regionHeight      the height of the raster region
     * @param levelImageSupport helps to compute the source pixels at level zero
     */
    public RasterDataEvalEnv(int offsetX, int offsetY, int regionWidth, int regionHeight,
                             LevelImageSupport levelImageSupport) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.regionWidth = regionWidth;
        this.regionHeight = regionHeight;
        this.levelImageSupport = levelImageSupport;
    }

    /**
     * Gets the x-offset of the raster region.
     *
     * @return the x-offset of the raster region
     */
    public int getOffsetX() {
        return offsetX;
    }

    /**
     * Gets the y-offset of the raster region.
     *
     * @return the y-offset of the raster region.
     */
    public int getOffsetY() {
        return offsetY;
    }

    /**
     * Gets the width of the raster region.
     *
     * @return the width of the raster region.
     */
    public int getRegionWidth() {
        return regionWidth;
    }

    /**
     * Gets the height of the raster region.
     *
     * @return the height of the raster region.
     */
    public int getRegionHeight() {
        return regionHeight;
    }

    /**
     * Gets the absolute pixel's x-coordinate within the data raster (from image at level zero in an image pyramid).
     *
     * @return the current source pixel's x-coordinate
     */
    public final int getPixelX() {
        return levelImageSupport.getSourceX(offsetX + elemIndex % regionWidth);
    }

    /**
     * Gets the absolute pixel's y-coordinate within the data raster (from image at level zero in an image pyramid).
     *
     * @return the current source pixel's y-coordinate
     */
    public final int getPixelY() {
        return levelImageSupport.getSourceY(offsetY + elemIndex / regionWidth);
    }

    /**
     * Gets the index of the current data element.
     *
     * @return the index of the current data element
     */
    public final int getElemIndex() {
        return elemIndex;
    }

    /**
     * Sets the index of the current data element.
     *
     * @param elemIndex the index of the current data element
     */
    public void setElemIndex(int elemIndex) {
        this.elemIndex = elemIndex;
    }
}
