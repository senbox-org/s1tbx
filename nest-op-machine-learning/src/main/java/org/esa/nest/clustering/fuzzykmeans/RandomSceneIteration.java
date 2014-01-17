/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.clustering.fuzzykmeans;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Rectangle;
import java.util.Random;

public class RandomSceneIteration {

    private final Operator operator;
    private final Random random;
    private final RasterDataNode[] rdn;
    private final int[] xValue;
    private final int[] yValue;
    private final int roiMemberCount;

    public RandomSceneIteration(Operator operator, RasterDataNode[] rdn, Roi roi, int seed) {
        this.operator = operator;
        this.rdn = rdn;
        random = new Random(seed);
        final int rasterWidth = rdn[0].getSceneRasterWidth();
        final int rasterHeight = rdn[0].getSceneRasterHeight();
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

    public int getRoiMemberCount() {
        return roiMemberCount;
    }

    public double[] getNextValue() {
        final double[] value = new double[rdn.length];

        final int x;
        final int y;
        if (xValue == null) {
            x = random.nextInt(rdn[0].getSceneRasterWidth());
            y = random.nextInt(rdn[0].getSceneRasterHeight());
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
