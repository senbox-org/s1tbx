package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.jai.SingleBandedSampleModel;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

/**
 * A {@link Tile} implementation backed by a {@link java.awt.image.WritableRaster}.
 */
public class TileImpl implements Tile {

    private final RasterDataNode rasterDataNode;
    private final WritableRaster writableRaster;
    private final int offsetX;
    private final int offsetY;
    private final int width;
    private final int height;
    private final int scanlineOffset;
    private final int scanlineStride;
    private final boolean target;
    private final byte[] rawSamplesByte;
    private final short[] rawSamplesShort;
    private final int[] rawSamplesInt;
    private final float[] rawSamplesFloat;
    private final double[] rawSamplesDouble;

    private ProductData sampleData;
    private boolean mustWriteSampleData;

    public TileImpl(RasterDataNode rasterDataNode, WritableRaster writableRaster, Rectangle rectangle) {
        this(rasterDataNode, writableRaster, rectangle, true);
    }

    public TileImpl(RasterDataNode rasterDataNode, WritableRaster writableRaster, Rectangle rectangle, boolean destination) {
        Assert.argument(writableRaster.getSampleModel() instanceof SingleBandedSampleModel, "writableRaster");
        SingleBandedSampleModel sm = (SingleBandedSampleModel) writableRaster.getSampleModel();
        DataBuffer db = writableRaster.getDataBuffer();
        Assert.argument(db.getNumBanks() == 1, "writableRaster");
        Object primitiveArray = ImageUtils.getPrimitiveArray(db);

        this.rasterDataNode = rasterDataNode;
        this.writableRaster = writableRaster;
        this.offsetX = rectangle.x;
        this.offsetY = rectangle.y;
        this.width = rectangle.width;
        this.height = rectangle.height;
        this.target = destination;
        this.scanlineStride = sm.getScanlineStride();
        this.scanlineOffset = offsetY * scanlineStride + offsetX + db.getOffset();
        this.rawSamplesByte = (primitiveArray instanceof byte[]) ? (byte[]) primitiveArray : null;
        this.rawSamplesShort = (primitiveArray instanceof short[]) ? (short[]) primitiveArray : null;
        this.rawSamplesInt = (primitiveArray instanceof int[]) ? (int[]) primitiveArray : null;
        this.rawSamplesFloat = (primitiveArray instanceof float[]) ? (float[]) primitiveArray : null;
        this.rawSamplesDouble = (primitiveArray instanceof double[]) ? (double[]) primitiveArray : null;
    }

    public boolean isTarget() {
        return target;
    }

    public Rectangle getRectangle() {
        return new Rectangle(offsetX, offsetY, width, height);
    }

    public int getMinX() {
        return offsetX;
    }

    public int getMinY() {
        return offsetY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public RasterDataNode getRasterDataNode() {
        return rasterDataNode;
    }

    public ProductData getRawSampleData() {
        if (sampleData == null) {
            synchronized (this) {
                if (checkRequestedAreaMatchesRasterArea()
                        && checkDataBufferSizeMatchesRasterArea()) {
                    // simply wrap existing data
                    sampleData = ProductData.createInstance(rasterDataNode.getDataType(), ImageUtils.getPrimitiveArray(writableRaster.getDataBuffer()));
                } else {
                    // create new instance
                    sampleData = rasterDataNode.createCompatibleRasterData(width, height);
                    if (target) {
                        // a target tile: must write sample values into raster later
                        mustWriteSampleData = true;
                    } else {
                        // a source tile: must also copy sample values
                        writableRaster.getDataElements(offsetX, offsetY,
                                                       width, height,
                                                       sampleData.getElems());
                    }
                }
            }
        }
        return sampleData;
    }

    public void setRawSampleData(ProductData sampleData) {
        Assert.notNull(sampleData, "sampleData");
        if (target) {
            if (sampleData != this.sampleData || mustWriteSampleData) {
                writableRaster.setDataElements(offsetX, offsetY,
                                               width, height,
                                               sampleData.getElems());
            }
        }
    }

    public byte[] getRawSamplesByte() {
        return rawSamplesByte;
    }

    public short[] getRawSamplesShort() {
        return rawSamplesShort;
    }

    public int[] getRawSamplesInt() {
        return rawSamplesInt;
    }

    public float[] getRawSamplesFloat() {
        return rawSamplesFloat;
    }

    public double[] getRawSamplesDouble() {
        return rawSamplesDouble;
    }

    public int getScanlineOffset() {
        return scanlineOffset;
    }

    public int getScanlineStride() {
        return scanlineStride;
    }

    public int getSampleInt(int x, int y) {
        return writableRaster.getSample(x, y, 0);
    }

    public void setSample(int x, int y, int v) {
        writableRaster.setSample(x, y, 0, v);
    }

    public float getSampleFloat(int x, int y) {
        return writableRaster.getSampleFloat(x, y, 0);
    }

    public void setSample(int x, int y, float v) {
        writableRaster.setSample(x, y, 0, v);
    }

    public double getSampleDouble(int x, int y) {
        return writableRaster.getSampleDouble(x, y, 0);
    }

    public void setSample(int x, int y, double v) {
        writableRaster.setSample(x, y, 0, v);
    }

    public boolean getSampleBoolean(int x, int y) {
        return writableRaster.getSample(x, y, 0) != 0;
    }

    public void setSample(int x, int y, boolean v) {
        writableRaster.setSample(x, y, 0, v ? 1 : 0);
    }

    private boolean checkRequestedAreaMatchesRasterArea() {
        return width == writableRaster.getWidth() && height == writableRaster.getHeight();
    }

    private boolean checkDataBufferSizeMatchesRasterArea() {
        return writableRaster.getDataBuffer().getSize() == writableRaster.getWidth() * writableRaster.getHeight();
    }
}
