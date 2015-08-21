package org.esa.snap.framework.datamodel;

import java.io.IOException;

/**
 * A time-coding based on an Array of modified julian days as double values. One MDJ per raster line.
 *
 * @author Sabine Embacher
 * @since SNAP 2.0
 */
public class RasterLineTimeCoding implements org.esa.snap.framework.datamodel.TimeCoding {

    private final double[] mjDs;
    private final int rasterWidth;
    private final int rasterHeight;

    public RasterLineTimeCoding(double[] mjDs, int rasterWidth, int rasterHeight) throws IOException {
        assert mjDs != null;
        assert rasterHeight == mjDs.length;
        this.mjDs = mjDs;
        this.rasterWidth = rasterWidth;
        this.rasterHeight = rasterHeight;
    }


    /**
     * Returns the time as MJD {@code double} for a raster line {@link PixelPos#getY()}.
     * If the x or y value of the pixel pos is outside the raster dimensions NaN will be returned.
     * @param pixelPos The pixel position in units of a given raster data node
     * @return
     */
    public double getMJD(PixelPos pixelPos) {
        double x = pixelPos.x;
        double y = pixelPos.y;
        if (x < 0 || x > rasterWidth
            || y < 0 || y > rasterHeight) {
            return Double.NaN;
        }
        int indexY = (int) Math.floor(y);
        if (indexY == rasterHeight) {
            indexY -= 1;
        }
        if (indexY < 0 || indexY >= mjDs.length) {
            return Double.NaN;
        }
        return mjDs[indexY];
    }
}
