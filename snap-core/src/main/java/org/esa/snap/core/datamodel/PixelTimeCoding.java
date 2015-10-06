package org.esa.snap.core.datamodel;

public class PixelTimeCoding implements TimeCoding {

    private final int rasterWidth;
    private final double[] timeMJD;
    private final int rasterHeight;

    public PixelTimeCoding(double[] timeMJD, int rasterWidth, int rasterHeight) {
        this.rasterWidth = rasterWidth;
        this.rasterHeight = rasterHeight;
        this.timeMJD = timeMJD;
    }

    @Override
    public double getMJD(PixelPos pixelPos) {
        double x = pixelPos.x;
        double y = pixelPos.y;
        if (x < 0 || x > rasterWidth
            || y < 0 || y > rasterHeight) {
            return Double.NaN;
        }
        int indexX = (int) x;
        int indexY = (int) y;
        if (indexX == rasterWidth) {
            indexX -= 1;
        }
        if (indexY == rasterHeight) {
            indexY -= 1;
        }

        int index = rasterWidth * indexY + indexX;
        if (index >= timeMJD.length) {
            return Double.NaN;
        }
        return timeMJD[index];
    }
}
