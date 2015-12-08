/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.cluster;

import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.Tile;

import java.awt.Rectangle;
import java.util.Random;

class RandomSceneIter {

    private final Operator operator;
    private final Random random;
    private final RasterDataNode[] rdn;
    private final int[] xValue;
    private final int[] yValue;
    private final int roiMemberCount;

    RandomSceneIter(Operator operator, RasterDataNode[] rdn, Roi roi, int seed) {
        this.operator = operator;
        this.rdn = rdn;
        random = new Random(seed);
        final int rasterWidth = rdn[0].getRasterWidth();
        final int rasterHeight = rdn[0].getRasterHeight();
        final int size = rasterWidth * rasterHeight;
        if (roi == null) {
            xValue = null;
            yValue = null;
            roiMemberCount = size;
        } else {
            xValue = new int[size];
            yValue = new int[size];
            int i = 0;
            for (int y = 0; y < rasterHeight; y++) {
                for (int x = 0; x < rasterWidth; x++) {
                    if (roi.contains(x, y)) {
                        xValue[i] = x;
                        yValue[i] = y;
                        i++;
                    }
                }
            }
            roiMemberCount = i;
        }
    }

    int getRoiMemberCount() {
        return roiMemberCount;
    }

    double[] getNextValue() {
        final double[] value = new double[rdn.length];

        final int x;
        final int y;
        if (xValue == null) {
            x = random.nextInt(rdn[0].getRasterWidth());
            y = random.nextInt(rdn[0].getRasterHeight());
        } else {
            final int randomIndex = random.nextInt(roiMemberCount);
            x = xValue[randomIndex];
            y = yValue[randomIndex];
        }
        final Rectangle rectangle = new Rectangle(x, y, 1, 1);
        for (int i = 0; i < rdn.length; i++) {
            final Tile sourceTile = operator.getSourceTile(rdn[i], rectangle);
            value[i] = sourceTile.getSampleDouble(x, y);
        }
        return value;
    }
}
