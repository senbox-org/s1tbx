/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.polarimetric.gpf.decompositions;

import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Tile;

import java.awt.*;

/**
 * Base class for polarimetric decompositions
 */
public class DecompositionBase {

    protected PolBandUtils.PolSourceBand[] srcBandList;
    protected final PolBandUtils.MATRIX sourceProductType;

    protected final int sourceImageWidth;
    protected final int sourceImageHeight;
    protected final int windowSizeX;
    protected final int windowSizeY;
    protected final int halfWindowSizeX;
    protected final int halfWindowSizeY;

    public enum TargetBandColour {R, G, B}

    public DecompositionBase(final PolBandUtils.PolSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                             final int windowSizeX, final int windowSizeY, final int srcImageWidth, final int srcImageHeight) {
        this.srcBandList = srcBandList;
        this.sourceProductType = sourceProductType;
        this.windowSizeX = windowSizeX;
        this.windowSizeY = windowSizeY;
        this.sourceImageWidth = srcImageWidth;
        this.sourceImageHeight = srcImageHeight;
        this.halfWindowSizeX = windowSizeX / 2;
        this.halfWindowSizeY = windowSizeY / 2;
    }

    /**
     * Get source tile rectangle.
     *
     * @param tx0 X coordinate for the upper left corner pixel in the target tile.
     * @param ty0 Y coordinate for the upper left corner pixel in the target tile.
     * @param tw  The target tile width.
     * @param th  The target tile height.
     * @return The source tile rectangle.
     */
    protected Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {
        final int x0 = Math.max(0, tx0 - halfWindowSizeX);
        final int y0 = Math.max(0, ty0 - halfWindowSizeY);
        final int xMax = Math.min(tx0 + tw - 1 + halfWindowSizeX, sourceImageWidth - 1);
        final int yMax = Math.min(ty0 + th - 1 + halfWindowSizeY, sourceImageHeight - 1);
        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;
        return new Rectangle(x0, y0, w, h);
    }

    /**
     * Convert pixel value from linear scale to dB.
     *
     * @param p       The pixel value in linear scale.
     * @param spanMin span min
     * @param spanMax span max
     * @return The pixel value in dB.
     */
    protected static double scaleDb(double p, final double spanMin, final double spanMax) {

        if (p < spanMin) {
            p = spanMin;
        }

        if (p > spanMax) {
            p = spanMax;
        }
        return 10.0 * Math.log10(p);
    }

    public static class MinMax {
        public double min = 1e+30;
        public double max = -min;
    }

    public static class TargetInfo {
        public final Tile tile;
        public final ProductData dataBuffer;
        public final TargetBandColour colour;

        public TargetInfo(final Tile tile, final TargetBandColour col) {
            this.tile = tile;
            this.dataBuffer = tile.getDataBuffer();
            this.colour = col;
        }
    }
}
