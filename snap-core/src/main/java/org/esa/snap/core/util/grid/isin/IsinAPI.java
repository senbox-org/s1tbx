package org.esa.snap.core.util.grid.isin;


/*
 Things we need from this package:

 - DONE: convert lon/lat into tile_x, tile_y, x, y
 - DONE: return dimension of specific tile
 - return projection params for each tile

 */

public class IsinAPI {

    private final Tile tile;

    public enum Raster {
        GRID_1_KM,
        GRID_500_M,
        GRID_250_M
    }

    private static final double TO_RAD = Math.PI / 180.0;

    /**
     * Constructs the API and initializes internal parameter according to the raster dimensions passed in.
     *
     * @param raster the raster dimension
     */
    public IsinAPI(Raster raster) {
        final ProjectionParam projectionParam = getProjectionParam(raster);

        tile = new Tile();
        tile.init(projectionParam);
    }

    /**
     * Map the location (lon/lat) to the global integerized sinusoidal raster.
     * The point returned contains the map x and y coordinates.
     *
     * @param lon longitude
     * @param lat latitude
     * @return the mapped location
     */
    public IsinPoint toGlobalMap(double lon, double lat) {
        return tile.forwardGlobalMap(lon * TO_RAD, lat * TO_RAD);
    }

    /**
     * Map the location (lon/lat) to the tiled global integerized sinusoidal raster.
     * The point returned contains the map x and y coordinates within the tile and the horizontal and vertical
     * (zero based) tile indices.
     *
     * @param lon longitude
     * @param lat latitude
     * @return the mapped location
     */
    public IsinPoint toTileImageCoordinates(double lon, double lat) {
        return tile.forwardTileImage(lon * TO_RAD, lat * TO_RAD);
    }

    /**
     * Retrieves the dimensions of a tile at the selected resolution.
     * The point returned contains the tile x and y dimensions.
     *
     * @return the tile dimensions
     */
    public IsinPoint getTileDimensions() {
        final long tile_height = tile.getNl_tile();
        final long tile_width = tile.getNs_tile();
        return new IsinPoint(tile_width, tile_height);
    }

    // @todo 1 tb/tb make static and test 2018-05-29
    private ProjectionParam getProjectionParam(Raster raster) {
        ProjectionType projectionType;
        if (raster == Raster.GRID_1_KM) {
            projectionType = ProjectionType.ISIN_K;
        } else if (raster == Raster.GRID_500_M) {
            projectionType = ProjectionType.ISIN_H;
        } else if (raster == Raster.GRID_250_M) {
            projectionType = ProjectionType.ISIN_Q;
        } else {
            throw new RuntimeException("Illegal projection type");
        }

        return ProjectionParamFactory.get(projectionType);
    }
}
