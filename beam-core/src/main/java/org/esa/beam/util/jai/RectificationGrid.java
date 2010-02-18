/*
 * $Id: RectificationGrid.java,v 1.1.1.1 2006/09/11 08:16:47 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util.jai;

import java.awt.geom.AffineTransform;

//@todo 1 se/** - add (more) class documentation

public class RectificationGrid {

    int width;
    int height;
    int numCoords;
    float[] sourceCoords;
    float[] destCoords;

    public RectificationGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.numCoords = width * height;
        int length = 2 * numCoords;
        this.sourceCoords = new float[length];
        this.destCoords = new float[length];
    }

    public static RectificationGrid createRectangular(int imgWidth,
                                                      int imgHeight,
                                                      int xStep,
                                                      int yStep) {

        int width = 1 + imgWidth / xStep;
        int height = 1 + imgHeight / yStep;
        RectificationGrid grid = new RectificationGrid(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = 2 * (y * width + x);
                grid.sourceCoords[index + 0] =
                grid.destCoords[index + 0] = x * xStep + 0.5F;
                grid.sourceCoords[index + 1] =
                grid.destCoords[index + 1] = y * yStep + 0.5F;
            }
        }
        return grid;
    }

    public static RectificationGrid createTestGrid(int imgWidth,
                                                   int imgHeight,
                                                   int xStep,
                                                   int yStep) {

        RectificationGrid grid = createRectangular(imgWidth, imgHeight, xStep, yStep);
        for (int y = 0; y < grid.height; y++) {
            for (int x = 0; x < grid.width; x++) {
                int index = 2 * (y * grid.width + x);
                float stretch = (float) y / (float) grid.height;
                stretch *= stretch;
                grid.destCoords[index] /= imgWidth;
                grid.destCoords[index] -= 0.5F;
                grid.destCoords[index] *= 0.6F + 0.2F * stretch;
                grid.destCoords[index] += 0.5F;
                grid.destCoords[index] *= imgWidth;
            }
        }
        AffineTransform af = AffineTransform.getRotateInstance(20.0 * Math.PI / 180.0,
                                                               0.5 * imgWidth,
                                                               0.5 * imgHeight);
        af.transform(grid.destCoords, 0, grid.destCoords, 0, grid.numCoords);
        return grid;
    }
}

