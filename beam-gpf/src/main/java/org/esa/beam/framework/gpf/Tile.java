package org.esa.beam.framework.gpf;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.internal.RasterImpl;

import java.awt.Rectangle;

/**
 * A tile can be cached by the {@link TileCache}. It wraps a {@link Raster} and the corresponding rectangular region.
 * Also it provides information about the {@link State}, the amount of memory allocated by the raster,
 * the time it was last accessed and the number of queries since it resides in the cache.
 */
public class Tile {

    /**
     * The default tile size.
     */
    public final static int DEFAULT_TILE_SIZE = 200;

    /**
     * The state in which the {@link Tile} resides.
     */
    public enum State {

        /**
         * The {@link Tile} is not yet computed.
         */
        NOT_COMPUTED,
        /**
         * The {@link Tile} is currently computing.
         */
        COMPUTING,
        /**
         * The {@link Tile} is already computed.
         */
        COMPUTED,
        /**
         * The {@link Tile} could not be computed.
         */
        ERROR
    }

    private long timeStamp;
    private long memorySize;
    private State state;
    private int queryCount;
    private Raster raster;
    private Rectangle rectangle;

    /**
     * Creates a new tile for the given rectangular region.
     *
     * @param rectangle a rectangular region
     */
    public Tile(Rectangle rectangle) {
        this.state = State.NOT_COMPUTED;
        this.rectangle = rectangle;
    }

    /**
     * Gets the {@link Raster} this tile wraps.
     *
     * @return the raster, may be {@code null}
     */
    public Raster getRaster() {
        return raster;
    }

    /**
     * Sets the {@link Raster} this tile should wrap.
     *
     * @param raster the raster
     */
    public void setRaster(Raster raster) {
        this.raster = raster;
        ProductData dataBuffer = raster.getDataBuffer();
        this.memorySize = (long) dataBuffer.getNumElems() * (long) dataBuffer.getElemSize();
        this.timeStamp = System.currentTimeMillis();
    }

    /**
     * Gets the rectangular region.
     *
     * @return the rectangular region
     */
    public Rectangle getRectangle() {
        return rectangle;
    }

    /**
     * Sets the rectangular region.
     *
     * @param rectangle the rectangular region
     */
    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    /**
     * Gets the {@link State} of the tile.
     *
     * @return the state
     */
    public State getState() {
        return state;
    }

    /**
     * Sets the {@link State} of the tile
     *
     * @param state the state
     */
    public void setState(State state) {
        Assert.notNull(state);
        this.state = state;
    }

    /**
     * Gets the size of memory allocated by the raster.
     *
     * @return the size of memory in bytes
     */
    public long getMemorySize() {
        return memorySize;
    }

    /**
     * Gets the time the tile was last queried from the {@link TileCache}.
     *
     * @return the time in milliseconds
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Gets the number of queries of this tile.
     *
     * @return the number of queries
     */
    public int getQueryCount() {
        return queryCount;
    }

    /**
     * Increases the counter of queries by one.
     */
    public void incQueryCount() {
        queryCount++;
        timeStamp = System.currentTimeMillis();
    }

    /**
     * Creates a new tile for the given {@link RasterDataNode} and rectangular region.
     * If the given {@link ProductData data buffer} is not {@code null} it is used for storing the data, otherwise a
     * new one is created.
     *
     * @param rasterDataNode the {@link RasterDataNode} to create the tile for
     * @param tileRectangle  the rectangular region to create the tile for
     * @param dataBuffer     the data buffer to use, may be {@code null}
     *
     * @return the newly created tile
     */
    public static Tile createTile(RasterDataNode rasterDataNode, Rectangle tileRectangle, ProductData dataBuffer) {
        if (dataBuffer == null) {
            dataBuffer = rasterDataNode.createCompatibleRasterData(tileRectangle.width, tileRectangle.height);
        }
        Raster raster = new RasterImpl(rasterDataNode, tileRectangle, dataBuffer);
        Tile tile = new Tile(tileRectangle);
        tile.setRaster(raster);
        return tile;
    }

    @Override
    public String toString() {
        RasterDataNode rasterDataNode = getRaster().getRasterDataNode();
        StringBuilder sb = new StringBuilder();
        sb.append("Tile[");
        sb.append("rect=");
        sb.append(getRectangle());
        sb.append(",state=");
        sb.append(getState());
        sb.append(",band=");
        sb.append(rasterDataNode.getName());
        sb.append(",reader=");
        sb.append(rasterDataNode.getProductReader());
        sb.append("]");
        return sb.toString();
    }
}
