package org.esa.beam.framework.datamodel;

import java.awt.Rectangle;

/**
 * @author Ralf Quast
 */
class DefaultSteppingFactory implements SteppingFactory {

    @Override
    public Stepping createStepping(Rectangle rectangle, int maxPointCount) {
        final int sw = rectangle.width;
        final int sh = rectangle.height;
        final int minX = rectangle.x;
        final int minY = rectangle.y;
        final int maxX = minX + sw - 1;
        final int maxY = minY + sh - 1;

        // Determine stepX and stepY so that maximum number of points is not exceeded
        int pointCountX = sw;
        int pointCountY = sh;
        int stepX = 1;
        int stepY = 1;

        // Adjust number of warp points to be considered so that a maximum of circa
        // maxPointCount points is not exceeded
        boolean adjustStepX = true;
        while (pointCountX * pointCountY > maxPointCount) {
            if (adjustStepX) {
                stepX++;
                pointCountX = sw / stepX + 1;
            } else {
                stepY++;
                pointCountY = sh / stepY + 1;
            }
            adjustStepX = !adjustStepX;
        }
        pointCountX = Math.max(1, pointCountX);
        pointCountY = Math.max(1, pointCountY);

        // Make sure we include the right border points,
        // if sw/stepX not divisible without remainder
        if (sw % stepX != 0) {
            pointCountX++;
        }
        // Make sure we include the bottom border points,
        // if sh/stepY not divisible without remainder
        if (sh % stepY != 0) {
            pointCountY++;
        }

        return new Stepping(minX, minY, maxX, maxY, pointCountX, pointCountY, stepX, stepY);
    }
}
