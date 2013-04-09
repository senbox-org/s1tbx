package org.esa.beam.binning.support;

import org.esa.beam.binning.PlanetaryGrid;

import java.io.IOException;

/**
 * Implementation of a reduced gaussian grid. It is often used in the climate modelling community.
 * <p/>
 * The grid points of a gaussian grid along each latitude are equally spaced. This means that the distance in degree
 * between two adjacent longitudes is the same.
 * Along the longitudes the grid points are not equally spaced. The distance varies along the meridian.
 * There are two types of the gaussian grid. The regular and the reduced grid.
 * While the regular grid has for each grid row the same number of columns, the number of columns varies in the reduced
 * grid type.
 *
 * @author Marco Peters
 * @see RegularGaussianGrid
 */
public class ReducedGaussianGrid implements PlanetaryGrid {

    private final GaussianGridConfig config;
    private final int numRows;
    private long numBins;
    private long lastBinIndex;
    private int lastRow;

    /**
     * Creates a new reduced gaussian grid.
     *
     * @param numRows the number of rows of the grid (from pole to pole)
     */
    public ReducedGaussianGrid(int numRows) {
        this.numRows = numRows;
        try {
            config = GaussianGridConfig.load(numRows / 2);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create gaussian grid: " + e.getMessage(), e);
        }
        int lastRowIndex = getNumRows() - 1;
        int lastRowStartIndex = config.getReducedFirstBinIndex(lastRowIndex);
        numBins = lastRowStartIndex + config.getReducedColumnCount(lastRowIndex);
    }

    @Override
    public long getBinIndex(double lat, double lon) {
        int rowIndex = findClosestInArray(config.getLatitudePoints(), lat);
        int colIndex = findClosestInArray(config.getReducedLongitudePoints(rowIndex), lon);
        return getFirstBinIndex(rowIndex) + colIndex;
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
    public int getNumRows() {
        return numRows;
    }

    @Override
    public int getNumCols(int rowIndex) {
        validateRowIndex(rowIndex);
        return config.getReducedColumnCount(rowIndex);
    }

    @Override
    public long getFirstBinIndex(int rowIndex) {
        validateRowIndex(rowIndex);
        return config.getReducedFirstBinIndex(rowIndex);
    }

    @Override
    public double getCenterLat(int rowIndex) {
        validateRowIndex(rowIndex);
        return config.getLatitude(rowIndex);
    }

    @Override
    public double[] getCenterLatLon(long bin) {
        int row = getRowIndex(bin);
        int col = getColumnIndex(bin);
        double latitude = getCenterLat(row);
        double longitude = config.getReducedLongitudePoints(row)[col];
        return new double[]{latitude, longitude};
    }

    private void validateRowIndex(int rowIndex) {
        int maxRowIndex = getNumRows() - 1;
        if (rowIndex > maxRowIndex) {
            String msg = String.format("Invalid row index. Maximum allowed is %d but was %d.", maxRowIndex, rowIndex);
            throw new IllegalArgumentException(msg);
        }
    }

    static int findClosestInArray(double[] array, double value) {
        double dist = Double.NaN;
        for (int row = 0; row < array.length; row++) {
            double rowLat = array[row];
            double currentDist = Math.abs(rowLat - value);
            if (currentDist > dist) {
                return row - 1; // previous row
            }
            dist = currentDist;
        }
        // if not yet found it is the last one
        return array.length - 1;
    }

    private int getColumnIndex(long bin) {
        int rowIndex = getRowIndex(bin);
        long firstBinIndex = getFirstBinIndex(rowIndex);
        return (int) (bin - firstBinIndex);
    }

}
