package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.ImageUtils;

import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * A {@link Tile} implementation backed by a {@link java.awt.image.WritableRaster}.
 */
public class TileImpl implements Tile {

    private final RasterDataNode rasterDataNode;
    private final Raster raster;
    private final WritableRaster writableRaster;
    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;
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

    public TileImpl(RasterDataNode rasterDataNode, Raster raster) {
        this(rasterDataNode, raster, new Rectangle(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight()), false);
    }

    public TileImpl(RasterDataNode rasterDataNode, Raster raster, Rectangle rectangle, boolean target) {
        Assert.notNull(rasterDataNode, "rasterDataNode");
        Assert.argument(raster.getNumBands() == 1, "raster");
        WritableRaster writableRaster = raster instanceof WritableRaster ? (WritableRaster) raster : null;
        if (target) {
            Assert.argument(writableRaster != null, "raster");
        }
        Assert.argument(raster.getSampleModel() instanceof ComponentSampleModel, "raster");
        ComponentSampleModel sm = (ComponentSampleModel) raster.getSampleModel();
        Assert.argument(sm.getNumBands() == 1, "raster");
        DataBuffer db = raster.getDataBuffer();
        Assert.argument(db.getNumBanks() == 1, "raster");
        Assert.notNull(rectangle, "rectangle");

        this.rasterDataNode = rasterDataNode;
        this.raster = raster;
        this.writableRaster = writableRaster;
        this.minX = rectangle.x;
        this.minY = rectangle.y;
        this.maxX = rectangle.x + rectangle.width - 1;
        this.maxY = rectangle.y + rectangle.height - 1;
        this.width = rectangle.width;
        this.height = rectangle.height;
        this.target = target;

        int smX0 = rectangle.x - raster.getSampleModelTranslateX();
        int smY0 = rectangle.y - raster.getSampleModelTranslateY();
        int dbI0 = db.getOffset();
        this.scanlineStride = sm.getScanlineStride();
        this.scanlineOffset = smY0 * scanlineStride + smX0 + dbI0;

        Object primitiveArray = ImageUtils.getPrimitiveArray(db);
        this.rawSamplesByte = (primitiveArray instanceof byte[]) ? (byte[]) primitiveArray : null;
        this.rawSamplesShort = (primitiveArray instanceof short[]) ? (short[]) primitiveArray : null;
        this.rawSamplesInt = (primitiveArray instanceof int[]) ? (int[]) primitiveArray : null;
        this.rawSamplesFloat = (primitiveArray instanceof float[]) ? (float[]) primitiveArray : null;
        this.rawSamplesDouble = (primitiveArray instanceof double[]) ? (double[]) primitiveArray : null;
    }

    public final boolean isTarget() {
        return target;
    }

    public final Rectangle getRectangle() {
        return new Rectangle(minX, minY, width, height);
    }

    public final int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public final int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public final RasterDataNode getRasterDataNode() {
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
                    if (isTarget()) {
                        // a target tile: must write sample values into raster later
                        mustWriteSampleData = true;
                    } else {
                        // a source tile: must also copy sample values
                        raster.getDataElements(minX, minY,
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
        if (isTarget()) {
            if (sampleData != this.sampleData || mustWriteSampleData) {
                writableRaster.setDataElements(minX, minY,
                                               width, height,
                                               sampleData.getElems());
            }
        }
    }

    public final byte[] getRawSamplesByte() {
        return rawSamplesByte;
    }

    public final short[] getRawSamplesShort() {
        return rawSamplesShort;
    }

    public final int[] getRawSamplesInt() {
        return rawSamplesInt;
    }

    public final float[] getRawSamplesFloat() {
        return rawSamplesFloat;
    }

    public final double[] getRawSamplesDouble() {
        return rawSamplesDouble;
    }

    public final int getScanlineOffset() {
        return scanlineOffset;
    }

    public final int getScanlineStride() {
        return scanlineStride;
    }

    public int getSampleInt(int x, int y) {
        return raster.getSample(x, y, 0);
    }

    public void setSample(int x, int y, int v) {
        writableRaster.setSample(x, y, 0, v);
    }

    public float getSampleFloat(int x, int y) {
        return raster.getSampleFloat(x, y, 0);
    }

    public void setSample(int x, int y, float v) {
        writableRaster.setSample(x, y, 0, v);
    }

    public double getSampleDouble(int x, int y) {
        return raster.getSampleDouble(x, y, 0);
    }

    public void setSample(int x, int y, double v) {
        writableRaster.setSample(x, y, 0, v);
    }

    public boolean getSampleBoolean(int x, int y) {
        return raster.getSample(x, y, 0) != 0;
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
