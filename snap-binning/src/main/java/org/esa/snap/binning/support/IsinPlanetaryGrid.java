package org.esa.snap.binning.support;

import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.core.util.grid.isin.IsinAPI;
import org.esa.snap.core.util.grid.isin.IsinPoint;

import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.GRID_1_KM;
import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.GRID_250_M;
import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.GRID_500_M;

public class IsinPlanetaryGrid implements PlanetaryGrid {

    private static final int NUM_TILES_VERTICAL = 18;
    private static final int NUM_TILES_HORIZONTAL = 36;

    private static final int NUM_ROWS_1KM = NUM_TILES_VERTICAL * 1200;
    private static final int NUM_ROWS_500M = NUM_TILES_VERTICAL * 2400;
    private static final int NUM_ROWS_250M = NUM_TILES_VERTICAL * 4800;

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
        final IsinPoint isinPoint = toIsinPoint(bin);
        final IsinPoint tileDimensions = isinAPI.getTileDimensions();
        return isinPoint.getTile_line() * (int) (tileDimensions.getY() + 0.5) + (int) (isinPoint.getY() + 0.5);
    }

    @Override
    public long getNumBins() {
        final IsinPoint tileDimensions = isinAPI.getTileDimensions();
        return (long) (tileDimensions.getX() + 0.5) * (long) (tileDimensions.getY() + 0.5) * NUM_TILES_VERTICAL * NUM_TILES_HORIZONTAL;
    }

    @Override
    public int getNumRows() {
        final IsinPoint tileDimensions = isinAPI.getTileDimensions();
        return (int) (tileDimensions.getY() + 0.5) * NUM_TILES_VERTICAL;
    }

    @Override
    public int getNumCols(int row) {
        final IsinPoint tileDimensions = isinAPI.getTileDimensions();
        return (int) (tileDimensions.getX() + 0.5) * NUM_TILES_HORIZONTAL;
    }

    @Override
    public long getFirstBinIndex(int row) {
        final IsinPoint tileDimensions = isinAPI.getTileDimensions();
        final int tileHeight = (int) (tileDimensions.getY() + 0.5);
        final int tileIdx = row / tileHeight;
        final int y = row - tileHeight * tileIdx;

        final IsinPoint isinPoint = new IsinPoint(0, y, 0, tileIdx);
        return toBinIndex(isinPoint);
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
        return 10000000000L * tile_y + 100000000L * tile_x + 10000L * y + x;
    }

    // this method is used in Calvalus - please keep it public tb 2018-06-08
    public static IsinPoint toIsinPoint(long binIndex) {
        final int tile_y = (int) (binIndex / 10000000000L);
        long partIndex = binIndex - 10000000000L * tile_y;
        final int tile_x = (int) (partIndex / 100000000L);
        partIndex = partIndex - 100000000L * tile_x;
        final int y = (int) (partIndex / 10000L);
        final int x = (int) (partIndex - 10000L * y);
        return new IsinPoint(x, y, tile_x, tile_y);
    }
}
