package org.esa.snap.binning.support;

/**
 * Implementation of a reduced gaussian grid.
 * <p>
 * A gaussian grid is a latitude/longitude grid. The spacing of the latitudes is not regular.
 * However, the spacing of the lines of latitude is symmetrical about the Equator. Note that
 * there is no latitude at either Pole or at the Equator. A grid is usually referred to by its
 * 'number' N, which is the number of lines of latitude between a Pole and the Equator.
 * <p>
 * The longitudes of the grid points are defined by giving the number of points along each
 * line of latitude. The first point is at longitude 0 and the points are equally spaced along
 * the line of latitude. In a regular gaussian grid, the number of longitude points along a
 * latitude is 4N. In a reduced gaussian grid, the number of longitude points along a latitude
 * is specified. Latitudes may have differing numbers of points but the grid is symmetrical
 * about the Equator.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @see RegularGaussianGrid
 */
public final class ReducedGaussianGrid extends AbstractGaussianGrid {

    private final long numBins;
    private long lastBinIndex;
    private int lastRow;

    /**
     * Creates a new reduced gaussian grid.
     *
     * @param numRows the number of rows of the grid (from pole to pole)
     */
    public ReducedGaussianGrid(int numRows) {
        super(numRows);
        final int lastRowIndex = getNumRows() - 1;
        final GaussianGridConfig config = getConfig();
        final int lastRowStartIndex = config.getReducedFirstBinIndex(lastRowIndex);
        numBins = lastRowStartIndex + config.getReducedColumnCount(lastRowIndex);
    }

    @Override
    public int getRowIndex(long binIndex) {
        if (binIndex == lastBinIndex) {
            return lastRow;
        }

        int minRow = 0;
        int maxRow = getNumRows() - 1;
        while (true) {
            int midRow = (minRow + maxRow) / 2;
            long lowBinIndex = getFirstBinIndex(midRow);
            long highBinIndex = lowBinIndex + (getNumCols(midRow) - 1);
            if (binIndex < lowBinIndex) {
                maxRow = midRow - 1;
            } else if (binIndex >= lowBinIndex && binIndex <= highBinIndex) {
                lastBinIndex = binIndex;
                lastRow = midRow;
                return midRow;
            } else if (binIndex > highBinIndex) {
                minRow = midRow + 1;
            }
        }
    }

    @Override
    public long getNumBins() {
        return numBins;
    }

    @Override
    protected int getNumColsUnchecked(int rowIndex) {
        return getConfig().getReducedColumnCount(rowIndex);
    }

    @Override
    protected long getFirstBinIndexUnchecked(int rowIndex) {
        return getConfig().getReducedFirstBinIndex(rowIndex);
    }

    @Override
    protected double getCenterLon(int rowIndex, int colIndex) {
        return getConfig().getReducedLongitudePoints(rowIndex)[colIndex];
    }

}
