package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.util.ImageUtils;

import java.awt.Rectangle;
import java.awt.image.WritableRaster;

/**
 * A {@link Raster} implementation backed by a {@link java.awt.image.WritableRaster}.
 */
public class TileImpl implements Raster {
    private RasterDataNode rasterDataNode;
    private WritableRaster writableRaster;
    private Rectangle rectangle;
    private ProductData sampleData;
    private boolean mustWriteSampleData;
    private boolean target;

    public TileImpl(RasterDataNode rasterDataNode, WritableRaster writableRaster, Rectangle rectangle) {
        this(rasterDataNode, writableRaster, rectangle, true);
    }

    public TileImpl(RasterDataNode rasterDataNode, WritableRaster writableRaster, Rectangle rectangle, boolean destination) {
        this.rasterDataNode = rasterDataNode;
        this.writableRaster = writableRaster;
        this.rectangle = rectangle;
        this.target = destination;
    }

    public boolean isTarget() {
        return target;
    }

    public Rectangle getRectangle() {
        return new Rectangle(rectangle);
    }

    public int getOffsetX() {
        return writableRaster.getMinX();
    }

    public int getOffsetY() {
        return writableRaster.getMinY();
    }

    public int getWidth() {
        return writableRaster.getWidth();
    }

    public int getHeight() {
        return writableRaster.getHeight();
    }

    public RasterDataNode getRasterDataNode() {
        return rasterDataNode;
    }

    public ProductData getDataBuffer() {
        if (sampleData == null) {
            synchronized (this) {
                if (checkRequestedAreaMatchesRasterArea()
                        && checkDataBufferSizeMatchesRasterArea()) {
                    // simply wrap existing data
                    sampleData = ProductData.createInstance(rasterDataNode.getDataType(), ImageUtils.getDataBufferArray(writableRaster.getDataBuffer()));
                } else {
                    // create new instance
                    sampleData = rasterDataNode.createCompatibleRasterData(rectangle.width, rectangle.height);
                    if (target) {
                        // a target tile: must write sample values into raster later
                        mustWriteSampleData = true;
                    } else {
                        // a source tile: must also copy sample values
                        writableRaster.getDataElements(rectangle.x, rectangle.y,
                                                       rectangle.width, rectangle.height,
                                                       sampleData.getElems());
                    }
                }
            }
        }
        return sampleData;
    }

    public void setSampleData(ProductData sampleData) {
        Assert.notNull(sampleData, "sampleData");
        if (target) {
            if (sampleData != this.sampleData || mustWriteSampleData) {
                writableRaster.setDataElements(rectangle.x, rectangle.y,
                                               rectangle.width, rectangle.height,
                                               sampleData.getElems());
            }
        }
    }

    private boolean checkRequestedAreaMatchesRasterArea() {
        return rectangle.width == writableRaster.getWidth()
                && rectangle.height == writableRaster.getHeight();
    }

    private boolean checkDataBufferSizeMatchesRasterArea() {
        return writableRaster.getDataBuffer().getSize() == writableRaster.getWidth() * writableRaster.getHeight();
    }

    public int getInt(int x, int y) {
        return writableRaster.getSample(x, y, 0);
    }

    public void setInt(int x, int y, int v) {
        writableRaster.setSample(x, y, 0, v);
    }

    public float getFloat(int x, int y) {
        return writableRaster.getSampleFloat(x, y, 0);
    }

    public void setFloat(int x, int y, float v) {
        writableRaster.setSample(x, y, 0, v);
    }

    public double getDouble(int x, int y) {
        return writableRaster.getSampleDouble(x, y, 0);
    }

    public void setDouble(int x, int y, double v) {
        writableRaster.setSample(x, y, 0, v);
    }

    public boolean getBoolean(int x, int y) {
        return writableRaster.getSample(x, y, 0) != 0;
    }

    public void setBoolean(int x, int y, boolean v) {
        writableRaster.setSample(x, y, 0, v ? 1 : 0);
    }
}
