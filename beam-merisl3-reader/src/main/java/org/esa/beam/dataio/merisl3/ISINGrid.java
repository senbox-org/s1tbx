package org.esa.beam.dataio.merisl3;

import java.awt.Point;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ISINGrid {

    /**
     * Earth equatorial radius in km.
     */
    public static final double RE = 6378.137;
    /**
     * Default number of latitude rows.
     */
    public static final int DEFAULT_ROW_COUNT = 2160; // number of latitude rows

    private final int rowCount;
    private final double binSize;
    private final double deltaLat;
    private final double[] lats;
    private final double[] deltaLons;
    private final int[] rowLength;
    private final int[] binOffsets;
    private final int totalBinCount;

    public ISINGrid(int rowCount) {
        this.rowCount = rowCount;
        binSize = Math.PI * RE / rowCount;
        deltaLat = 180.0 / rowCount;

        /* effective size (km and deg) of a cell in latitudinal direction */
        /* computes the number of cells in longitudinal direction for each rowIndex */
        /* and the bin effective longitudinal size */
        lats = new double[rowCount];
        deltaLons = new double[rowCount];
        rowLength = new int[rowCount];
        binOffsets = new int[rowCount];

        int binCount = 0;
        for (int i = 0; i < rowCount; i++) {
            lats[i] = -90.0 + (i + 0.5) * deltaLat;
            rowLength[i] = (int) (0.5 + 2.0 * Math.PI * RE * Math.cos(Math.toRadians(lats[i])) / binSize);
            deltaLons[i] = 360.0 / rowLength[i];
            binOffsets[i] = (i == 0) ? 0 : binOffsets[i - 1] + rowLength[i - 1];
            binCount += rowLength[i];
        }
        totalBinCount = binCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public double getBinSize() {
        return binSize;
    }

    public double getDeltaLat() {
        return deltaLat;
    }

    public int getTotalBinCount() {
        return totalBinCount;
    }

    public double getLat(int rowIndex) {
        return lats[rowIndex];
    }

    public double getDeltaLon(int rowIndex) {
        return deltaLons[rowIndex];
    }

    public int getRowLength(int rowIndex) {
        return rowLength[rowIndex];
    }

    public int getBinOffset(int rowIndex) {
        return binOffsets[rowIndex];
    }

    public int getRowIndex(int binIndex) {
        final int[] binOffsets = this.binOffsets;
        final int totalBinCount = this.totalBinCount;

        int iL = 0;
        int iU = rowCount - 1;
        int iM;

        if (binIndex < 0 || binIndex >= totalBinCount) {
            return -1;
        }

        do {
// todo - check if this is an optimization or not
//            if (binIndex == binOffsets[iL]) {
//                return iL;
//            }
//            if (binIndex == binOffsets[iU]) {
//                return iU;
//            }
            iM = (iL + iU - 1) / 2;
            if (binIndex < binOffsets[iM]) {
                iU = iM;
            } else if (binIndex >= binOffsets[iM + 1]) {
                iL = iM + 1;
            } else {
                return iM;
            }
        } while (iL != iU);

        return iL;
    }

    public Point getGridPoint(int binIndex, Point gridPoint) {
        //gridPoint = (gridPoint == null) ? new Point() : gridPoint;
        final int rowIndex = getRowIndex(binIndex);
        gridPoint.x = (rowIndex == -1) ? -1 : (binIndex - binOffsets[rowIndex]);
        gridPoint.y = rowIndex;
        return gridPoint;
    }

    public int getBinIndex(Point gridPoint) {
        final int colIndex = gridPoint.x;
        final int rowIndex = gridPoint.y;
        if (rowIndex >= 0 && rowIndex < rowCount) {
            if (colIndex >= 0 && colIndex < rowLength[rowIndex]) {
                return binOffsets[rowIndex] + colIndex;
            }
        }
        return -1;
    }

    /**
     * Gets the zero-based column index in the global ISIN grid for the given zero-based row index and longitude.
     *
     * @param rowIndex the zero-based row index in the range 0...{@link #getRowCount()}-1
     * @param lon      the longitude in the range 0...360 degree
     *
     * @return the zero-based column index only if the longitude is in the range 0...360 degree, otherwise undefined
     *
     * @throws ArrayIndexOutOfBoundsException if the rowIndex in out of bounds
     */
    public int getColIndex(int rowIndex, double lon) {
        return (int) (lon / getDeltaLon(rowIndex) + 0.5);
    }

    /**
     * Gets the zero-based bin index in the global ISIN grid for the given zero-based row index and longitude.
     *
     * @param rowIndex the zero-based row index in the range 0...{@link #getRowCount()}-1
     * @param lon      the longitude in the range 0...360 degree
     *
     * @return the zero-based bin index only if the longitude is in the range 0...360 degree, otherwise undefined
     *
     * @throws ArrayIndexOutOfBoundsException if the rowIndex in out of bounds
     */
    public int getBinIndex(int rowIndex, double lon) {
        final int colIndex = getColIndex(rowIndex, lon);
        return getBinOffset(rowIndex) + colIndex;
    }
    /**
     * Detects the row count from the product name.
     * 
     * @param productName the name of the L3 product
     * 
     * @return the row count
     */
    public static int detectRowCount(String productName) {
        Pattern p = Pattern.compile(".*_(\\d{4})x(\\d{4})_.*");
        Matcher m = p.matcher(productName);
        if (m.matches() && m.groupCount() == 2) {
            String binSize1 = m.group(1);
            String binSize2 = m.group(2);
            if (binSize1.equals(binSize2)) {
                int binSize = Integer.parseInt(binSize1);
                return (int) Math.round(Math.PI * RE * 1000/ binSize);
            }
        }
        return DEFAULT_ROW_COUNT;
    }
}
