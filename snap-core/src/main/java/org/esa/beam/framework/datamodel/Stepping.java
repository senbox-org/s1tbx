package org.esa.beam.framework.datamodel;

/**
 * @author Ralf Quast
 */
final class Stepping {

    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;
    private final int pointCountX;
    private final int pointCountY;
    private final int stepX;
    private final int stepY;

    Stepping(int minX, int minY, int maxX, int maxY, int pointCountX, int pointCountY, int stepX, int stepY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.pointCountX = pointCountX;
        this.pointCountY = pointCountY;
        this.stepX = stepX;
        this.stepY = stepY;
    }

    int getMinX() {
        return minX;
    }

    int getMaxX() {
        return maxX;
    }

    int getMinY() {
        return minY;
    }

    int getMaxY() {
        return maxY;
    }

    int getPointCountX() {
        return pointCountX;
    }

    int getPointCountY() {
        return pointCountY;
    }

    int getStepX() {
        return stepX;
    }

    int getStepY() {
        return stepY;
    }

    int getPointCount() {
        return pointCountX * pointCountY;
    }
}
