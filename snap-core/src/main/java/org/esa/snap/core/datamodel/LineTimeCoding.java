package org.esa.snap.core.datamodel;

/**
 * A time-coding based on an Array of modified julian days as double values. One MDJ per raster line.
 *
 * @author Sabine Embacher
 * @since SNAP 2.0
 */
public class LineTimeCoding implements TimeCoding {

    private final double[] mjDs;
    private final double minY;
    private final double maxY;

    public LineTimeCoding(int numLines, double mjdStart, double mjdEnd) {
        this(createMJDs(numLines, mjdStart, mjdEnd));
    }

    public LineTimeCoding(double[] mjDs) {
        assert mjDs != null;
        assert mjDs.length > 0;
        this.mjDs = mjDs;
        this.minY = 0.0;
        this.maxY = mjDs.length;
    }

    /**
     * Gets the time as MJD {@code double} for a raster line {@link PixelPos#getY()}.
     * If the y value of the pixel pos is outside the raster y dimension NaN will be returned.
     * @param pixelPos The pixel position in units of a given raster data node
     * @return the time as MJD {@code double}
     */
    public double getMJD(PixelPos pixelPos) {
        double y = pixelPos.y;
        if (y < minY || y > maxY) {
            return Double.NaN;
        }
        int indexY = (int) Math.floor(y);
        if (indexY == maxY) {
            indexY -= 1;
        }
        return mjDs[indexY];
    }

    private static double[] createMJDs(int numLines, double mjdStart, double mjdEnd) {
        final double part = (mjdEnd - mjdStart) / (numLines - 1);
        final double[] MJDs = new double[numLines];
        MJDs[0] = mjdStart;
        MJDs[MJDs.length - 1] = mjdEnd;
        for (int i = 1; i < MJDs.length - 1; i++) {
            MJDs[i] = mjdStart + i * part;
        }
        return MJDs;
    }
}
