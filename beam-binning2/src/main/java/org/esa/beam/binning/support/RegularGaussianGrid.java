package org.esa.beam.binning.support;

import org.esa.beam.binning.PlanetaryGrid;

import java.io.IOException;

/**
 * Implementation of a regular gaussian grid. It is often used in the climate modelling community.
 * <p/>
 * The grid points of a gaussian grid along each latitude are equally spaced. This means that the distance in degree
 * between two adjacent longitudes is the same.
 * Along the longitudes the grid points are not equally spaced. The distance varies along the meridian.
 * There are two types of the gaussian grid. The regular and the reduced grid.
 * While the regular grid has for each grid row the same number of columns, the number of columns varies in the reduced
 * grid type.
 *
 * @author Marco Peters
 * @see ReducedGaussianGrid
 */
public class RegularGaussianGrid implements PlanetaryGrid {

    private final GaussianGridConfig config;
    private final int numRows;

    /**
     * Creates a new regular gaussian grid.
     *
     * @param numRows the number of rows of the grid (from pole to pole)
     */
    public RegularGaussianGrid(int numRows) {
        this.numRows = numRows;
        try {
            config = GaussianGridConfig.load(numRows / 2);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create gaussian grid: " + e.getMessage(), e);
        }
    }

    @Override
    public long getBinIndex(double lat, double lon) {
        int rowIndex = findClosestInArray(config.getLatitudePoints(), lat);
        int colIndex = findClosestInArray(config.getRegularLongitudePoints(), lon);
        return getFirstBinIndex(rowIndex) + colIndex;
    }

    @Override
    public int getRowIndex(long binIndex) {
        return (int) (binIndex / (config.getRegularColumnCount()));
    }

    @Override
    public long getNumBins() {
        return getNumRows() * config.getRegularColumnCount();
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    @Override
    public int getNumCols(int rowIndex) {
        validateRowIndex(rowIndex);
        return config.getRegularColumnCount();
    }

    @Override
    public long getFirstBinIndex(int rowIndex) {
        validateRowIndex(rowIndex);
        return rowIndex * getNumCols(rowIndex);
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
        double longitude = config.getRegularLongitudePoints()[col];

        return new double[]{latitude, longitude};
    }

    private void validateRowIndex(int rowIndex) {
        int maxRowIndex = getNumRows() - 1;
        if (rowIndex > maxRowIndex) {
            String msg = String.format("Invalid row index. Maximum allowed is %d, but was %d.", maxRowIndex, rowIndex);
            throw new IllegalArgumentException(msg);
        }
    }

    static int findClosestInArray(double[] array, double value) {
        double dist = Double.NaN;
        for (int i = 0; i < array.length; i++) {
            double arrayValue = array[i];
            double currentDist = Math.abs(arrayValue - value);
            if (currentDist > dist) {
                return i - 1; // previous in array
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
