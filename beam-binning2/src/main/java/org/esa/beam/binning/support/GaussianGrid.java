package org.esa.beam.binning.support;

import org.esa.beam.binning.PlanetaryGrid;

import java.io.IOException;

/**
 * Implementation of a gaussian grid. It is often used in the climate modelling community.
 * <p/>
 * The grid points of a gaussian grid along each latitude are equally spaced. This means that the distance in degree
 * between two adjacent longitudes is the same.
 * Along the longitudes the grid points are not equally spaced. The distance varies along the meridian.
 * There are two types of the gaussian grid. The regular and the reduced grid.
 * While the regular grid has for each grid row the same number of columns, the number of columns varies in the reduced
 * grid type.
 *
 * @author Marco Peters
 */
public class GaussianGrid implements PlanetaryGrid {

    private final Number gaussianGridNumber;
    private final boolean reduced;
    private final GaussianGridConfig gridConfig;

    /**
     * Creates a new gaussian grid.
     *
     * @param gaussianGridNumber the grid number specifying the number of rows between a pole and the equator
     * @param reduced            whether the created grid shall be of reduced type or not
     */
    public GaussianGrid(Number gaussianGridNumber, boolean reduced) {
        this.gaussianGridNumber = gaussianGridNumber;
        this.reduced = reduced;
        try {
            gridConfig = GaussianGridConfig.load(gaussianGridNumber);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create gaussian grid: " + e.getMessage(), e);
        }
    }

    public Number getGaussianGridNumber() {
        return gaussianGridNumber;
    }

    public boolean isReduced() {
        return reduced;
    }

    @Override
    public long getBinIndex(double lat, double lon) {
        int rowIndex = findClosestInArray(gridConfig.getLatitudePoints(), lat);
        int colIndex;
        if (reduced) {
            colIndex = findClosestInArray(gridConfig.getReducedLongitudePoints(rowIndex), lon);
        } else {
            colIndex = findClosestInArray(gridConfig.getRegularLongitudePoints(), lon);
        }
        return getFirstBinIndex(rowIndex) + colIndex;
    }

    @Override
    public int getRowIndex(long binIndex) {
        if (reduced) {
            for (int i = 0; i < getNumRows(); i++) {
                long firstBinIndex = getFirstBinIndex(i);
                int cols = gridConfig.getReducedColumnCount(i);
                if (binIndex <= firstBinIndex + cols) {
                    return i;
                }
            }
        } else {
            return (int) (binIndex / getNumCols());
        }
        return -1;
    }

    @Override
    public long getNumBins() {
        if (reduced) {
            int lastRowIndex = getNumRows() - 1;
            int lastRowStartIndex = gridConfig.getReducedFirstBinIndex(lastRowIndex);
            return lastRowStartIndex + gridConfig.getReducedColumnCount(lastRowIndex);
        } else {
            return getNumRows() * getNumCols();
        }
    }

    @Override
    public int getNumRows() {
        return gaussianGridNumber.getRowCount() * 2;
    }

    @Override
    public int getNumCols(int rowIndex) {
        validateRowIndex(rowIndex);
        if (reduced) {
            return gridConfig.getReducedColumnCount(rowIndex);
        } else {
            return gridConfig.getRegularColumnCount();
        }
    }

    @Override
    public long getFirstBinIndex(int rowIndex) {
        if (reduced) {
            return gridConfig.getReducedFirstBinIndex(rowIndex);
        } else {
            return rowIndex * getNumCols(rowIndex);
        }
    }

    @Override
    public double getCenterLat(int rowIndex) {
        validateRowIndex(rowIndex);
        return gridConfig.getLatitude(rowIndex);
    }

    @Override
    public double[] getCenterLatLon(long bin) {
        int row = getRowIndex(bin);
        int col = getColumnIndex(bin);
        double latitude = getCenterLat(row);
        double longitude;
        if (reduced) {
            longitude = gridConfig.getReducedLongitudePoints(row)[col];
        } else {
            longitude = gridConfig.getRegularLongitudePoints()[col];

        }
        return new double[]{latitude, longitude};
    }

    private void validateRowIndex(int rowIndex) {
        int maxRowIndex = getNumRows() - 1;
        if (rowIndex > maxRowIndex) {
            String msg = String.format("Invalid row index. Maximum allowed is %d but was %d.", maxRowIndex, rowIndex);
            throw new IllegalArgumentException(msg);
        }
    }

    private int findClosestInArray(double[] array, double value) {
        double dist = Double.NaN;
        for (int row = 0; row < array.length; row++) {
            double rowLat = array[row];
            double currentDist = Math.abs(rowLat - value);
            if (currentDist > dist) {
                return row - 1; // previous row
            }
            dist = currentDist;
        }
        return -1;
    }

    private int getColumnIndex(long bin) {
        int rowIndex = getRowIndex(bin);
        long firstBinIndex = getFirstBinIndex(rowIndex);
        return (int) (bin - firstBinIndex);
    }

    private int getNumCols() {
        return gaussianGridNumber.getRowCount() * 4;
    }


    public static enum Number {
        N32(32),
        N48(48),
        N80(80),
        N128(128),
        N160(160),
        N200(200),
        N256(256),
        N320(320),
        N400(400),
        N512(512),
        N640(640);

        private final int rowCount;

        /**
         * Number of rows between the Pole and the Equator.
         *
         * @param rowCount the number of rows
         */
        private Number(int rowCount) {
            this.rowCount = rowCount;
        }

        public int getRowCount() {
            return rowCount;
        }
    }
}
