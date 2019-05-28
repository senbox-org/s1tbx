package org.esa.snap.core.gpf.common.reproject;

import org.junit.Test;

import java.awt.Rectangle;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class WarpFromSourceCoordinatesTest {

    @Test
    public void mapDestRect() {


        WarpFromSourceCoordinates warp = new TestWarpFromSourceCoordinates(new Rectangle(5, 10, 50, 50));

        Rectangle outerRect = new Rectangle(0, 0, 100, 100);

        Rectangle rectangle = warp.mapDestRect(outerRect);
        // expected result does not include the whole data area but only the first data column.
        // This is sufficient to make the
        assertEquals(-1, rectangle.x);
        assertEquals(-1, rectangle.y);
        assertEquals(7, rectangle.width);
        assertEquals(61, rectangle.height);
        System.out.println("rectangle = " + rectangle);
    }

    private static class TestWarpFromSourceCoordinates extends WarpFromSourceCoordinates {

        private final Rectangle innerValidRect;

        public TestWarpFromSourceCoordinates(Rectangle innerValidRect) {
            super(null);
            this.innerValidRect = innerValidRect;
        }

        @Override
        public float[] warpSparseRect(int xmin, int ymin, int width, int height, int periodX, int periodY, float[] destRect) {
            final int xmax = xmin + width;
            final int ymax = ymin + height;
            final int count = ((width + (periodX - 1)) / periodX) * ((height + (periodY - 1)) / periodY);
            if (destRect == null) {
                destRect = new float[2 * count];
            }

            int index = 0;
            for (int y = ymin; y < ymax; y += periodY) {
                for (int x = xmin; x < xmax; x += periodX) {
                    if (innerValidRect.contains(x, y)) {
                        destRect[index++] = x;
                        destRect[index++] = y;
                    } else {
                        destRect[index++] = -1;
                        destRect[index++] = -1;
                    }
                }

            }
            return destRect;
        }
    }
}