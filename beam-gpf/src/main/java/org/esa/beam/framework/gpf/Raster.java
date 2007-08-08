package org.esa.beam.framework.gpf;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.Rectangle;

/**
 * A 2-dimensional wrapper for {@link ProductData}.
 */
public interface Raster {

    /**
     * The tile rectangle in raster coordinates.
     *
     * @return the tile rectangle
     */
    Rectangle getRectangle();

    /**
     * Gets the x-offset of the {@link Rectangle} within the {@link RasterDataNode}.
     *
     * @return the x-offset
     */
    int getOffsetX();

    /**
     * Gets the y-offset of the {@link Rectangle} within the {@link RasterDataNode}.
     *
     * @return the y-offset
     */
    int getOffsetY();

    /**
     * Gets the width of the {@link Rectangle} within the {@link RasterDataNode}.
     *
     * @return the width
     */
    int getWidth();

    /**
     * Gets the height of the {@link Rectangle} within the {@link RasterDataNode}.
     *
     * @return the height
     */
    int getHeight();

    /**
     * The raster dataset to which this raster belongs to.
     *
     * @return the raster data node of a data product, e.g. a {@link org.esa.beam.framework.datamodel.Band} or
     *         {@link org.esa.beam.framework.datamodel.TiePointGrid TiePointGrid}.
     */
    RasterDataNode getRasterDataNode();

    /**
     * Gets the data buffer which is wrapped by this raster.
     *
     * @return the data buffer
     */
    ProductData getDataBuffer();

    /**
     * Gets the integer value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     *
     * @return the integer value
     */
    int getInt(int x, int y);

    /**
     * Sets the integer value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @param v the integer value
     */
    void setInt(int x, int y, int v);

    /**
     * Gets the float value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     *
     * @return the float value
     */
    float getFloat(int x, int y);

    /**
     * Sets the float value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @param v the float value
     */
    void setFloat(int x, int y, float v);

    /**
     * Gets the double value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     *
     * @return the double value
     */
    double getDouble(int x, int y);

    /**
     * Sets the double value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @param v the double value
     */
    void setDouble(int x, int y, double v);

    /**
     * Gets the boolean value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     *
     * @return the boolean value
     */
    boolean getBoolean(int x, int y);

    /**
     * Sets the boolean value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @param v the boolean value
     */
    void setBoolean(int x, int y, boolean v);
}
