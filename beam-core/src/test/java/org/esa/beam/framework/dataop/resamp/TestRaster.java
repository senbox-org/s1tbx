/*
 * $Id: TestRaster.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.resamp;

class TestRaster implements Resampling.Raster {

    float[][] array = new float[][]{
        new float[]{10, 20, 30, 40, 50},
        new float[]{30, 20, 30, 20, 30},
        new float[]{10, 40, 20, 30, 70},
        new float[]{20, 30, 40, 60, 80},
        new float[]{10, 40, 10, 90, 70},
    };

    public int getWidth() {
        return array[0].length;
    }

    public int getHeight() {
        return array.length;
    }

    public float getSample(int x, int y) {
        return array[y][x];
    }
}
