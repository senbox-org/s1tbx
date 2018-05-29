package org.esa.snap.binning.support;

import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.core.util.grid.isin.IsinAPI;
import org.esa.snap.core.util.grid.isin.IsinPoint;

import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.GRID_1_KM;
import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.GRID_250_M;
import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.GRID_500_M;

public class IsinPlanetaryGrid implements PlanetaryGrid {

    private static final int NUM_ROWS_1KM = 18 * 1200;
    private static final int NUM_ROWS_500M = 18 * 2400;
    private static final int NUM_ROWS_250M = 18 * 4800;
    private final IsinAPI isinAPI;


    public IsinPlanetaryGrid(int numRows) {
        if (numRows == NUM_ROWS_1KM) {
            isinAPI = new IsinAPI(GRID_1_KM);
        } else if (numRows == NUM_ROWS_500M) {
            isinAPI = new IsinAPI(GRID_500_M);
        } else if (numRows == NUM_ROWS_250M) {
            isinAPI = new IsinAPI(GRID_250_M);
        } else {
            throw new IllegalArgumentException("Invalid number of rows");
        }
    }

    @Override
    public long getBinIndex(double lat, double lon) {
        final IsinPoint isinPoint = isinAPI.toTileImageCoordinates(lon, lat);

        return toBinIndex(isinPoint);
    }

    @Override
    public int getRowIndex(long bin) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public long getNumBins() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int getNumRows() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int getNumCols(int row) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public long getFirstBinIndex(int row) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public double getCenterLat(int row) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public double[] getCenterLatLon(long bin) {
        throw new RuntimeException("not implemented");
    }

    static long toBinIndex(IsinPoint point) {
        final short x = (short) (point.getX() + 0.5);
        final short y = (short) (point.getY() + 0.5);
        final int tile_x = point.getTile_col();
        final int tile_y = point.getTile_line();
        return 100000000L * x + 10000 * y + 100 * tile_x + tile_y;
    }

    static IsinPoint toIsinPoint(long binIndex) {
        final int x = (int) (binIndex / 100000000L);
        long partIndex = binIndex - 100000000L * x;
        final int y = (int) (partIndex / 10000);
        partIndex = partIndex - 10000 * y;
        final int tile_x = (int) (partIndex / 100);
        final int tile_y = (int) (partIndex - 100 * tile_x);
        return new IsinPoint(x, y, tile_x, tile_y);
    }
}
