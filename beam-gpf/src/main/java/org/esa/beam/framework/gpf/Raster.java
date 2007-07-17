package org.esa.beam.framework.gpf;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.Rectangle;

/**
 * A 2-dimensional wrapper for {@link ProductData}.
 */
public interface Raster {
	
	/**
     * The tile rectangle in raster coordinates.
     * @return the tile rectangle
     */
    Rectangle getRectangle();

    int getOffsetX();

    int getOffsetY();

    int getWidth();

    int getHeight();
    
    /**
     * The raster dataset to which this tile belongs to.
     * @return the raster data node of a data product, e.g. a {@link Band Band} or {@link org.esa.beam.framework.datamodel.TiePointGrid TiePointGrid}.
     */
    RasterDataNode getRasterDataNode();

    ProductData getDataBuffer();

    int getInt(int x, int y);

    void setInt(int x, int y, int v);

    float getFloat(int x, int y);

    void setFloat(int x, int y, float v);

    double getDouble(int x, int y);

    void setDouble(int x, int y, double v);

    boolean getBoolean(int x, int y);
    
    void setBoolean(int x, int y, boolean v);
}
