package org.esa.beam.binning.support;

import org.esa.beam.binning.PlanetaryGrid;

import java.io.IOException;

/**
 * An abstract base class for Gaussian grids.
 * <p/>
 * A gaussian grid is a latitude/longitude grid. The spacing of the latitudes is not regular.
 * However, the spacing of the lines of latitude is symmetrical about the Equator. Note that
 * there is no latitude at either Pole or at the Equator. A grid is usually referred to by its
 * 'number' N, which is the number of lines of latitude between a Pole and the Equator.
 * <p/>
 * The longitudes of the grid points are defined by giving the number of points along each
 * line of latitude. The first point is at longitude 0 and the points are equally spaced along
 * the line of latitude. In a regular gaussian grid, the number of longitude points along a
 * latitude is 4N. In a reduced gaussian grid, the number of longitude points along a latitude
 * is specified. Latitudes may have differing numbers of points but the grid is symmetrical
 * about the Equator.
 *
 * @author Ralf Quast
 */
abstract class AbstractGaussianGrid implements PlanetaryGrid {

    private final int numRows;
    private final GaussianGridConfig config;

    protected AbstractGaussianGrid(int numRows) {
        this.numRows = numRows;
        try {
            config = GaussianGridConfig.load(numRows / 2);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create gaussian grid: " + e.getMessage(), e);
        }
    }

    protected final GaussianGridConfig getConfig() {
        return config;
    }

    @Override
    public final long getBinIndex(double lat, double lon) {
        final int rowIndex = getRowIndex(lat);
        final int colIndex = getColIndex(rowIndex, lon);

        return getFirstBinIndex(rowIndex) + colIndex;
    }

    @Override
    public final int getNumRows() {
        return numRows;
    }

    @Override
    public final int getNumCols(int rowIndex) {
        validateRowIndex(rowIndex);
        return getNumColsUnchecked(rowIndex);
    }

    @Override
    public final long getFirstBinIndex(int rowIndex) {
        validateRowIndex(rowIndex);
        return getFirstBinIndexUnchecked(rowIndex);
    }

    @Override
    public final double getCenterLat(int rowIndex) {
        validateRowIndex(rowIndex);
        return config.getLatitude(rowIndex);
    }

    @Override
    public final double[] getCenterLatLon(long binIndex) {
        final int rowIndex = getRowIndex(binIndex);
        final int colIndex = getColIndex(binIndex, rowIndex);
        final double lat = getCenterLat(rowIndex);
        final double lon = getCenterLon(rowIndex, colIndex);

        return new double[]{lat, lon > 180.0 ? lon - 360.0 : lon};
    }

    protected abstract int getNumColsUnchecked(int rowIndex);

    protected abstract long getFirstBinIndexUnchecked(int rowIndex);

    protected abstract double getCenterLon(int rowIndex, int colIndex);

    private int getRowIndex(double lat) {
        return findNearest(config.getLatitudePoints(), lat);
    }

    private int getColIndex(int rowIndex, double lon) {
        int numColsInRow = getNumColsUnchecked(rowIndex);
        final double delta = 360.0 / numColsInRow;
        if (lon < 0.0) {
            lon += 360.0;
        }
        int index = (int) (lon / delta + 0.5);
        return index == numColsInRow ? 0 : index;
    }

    private int getColIndex(long binIndex, int rowIndex) {
        return (int) (binIndex - getFirstBinIndex(rowIndex));
    }

    private void validateRowIndex(int rowIndex) {
        int maxRowIndex = getNumRows() - 1;
        if (rowIndex > maxRowIndex) {
            String msg = String.format("Invalid row index. Maximum allowed is %d, but was %d.", maxRowIndex, rowIndex);
            throw new IllegalArgumentException(msg);
        }
    }

    // package public for testing only
    static int findNearest(double[] values, double value) {
        int l = 0;
        int h = values.length - 1;

        // binary search: values must be sorted in descending order
        while (l <= h) {
            final int m = (h + l) >> 1;
            if (values[m] >= value) {
                l = m + 1;
            } else {
                h = m - 1;
            }
        }

        if (h == -1) { // value is less than the least value in the array
            return 0;
        } else if (h == values.length - 1) { // value is greater than the greatest value in the array
            return h;
        } else if (values[h] - value < value - values[h + 1]) {
            return h;
        } else {
            return h + 1;
        }
    }

}
