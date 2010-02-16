package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.ImageUtils;

import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Iterator;

/**
 * A {@link Tile} implementation backed by a {@link java.awt.image.Raster}.
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
    private final boolean target;
    private final boolean scalingApplied;
    private final int scanlineOffset;
    private final int scanlineStride;
    private final byte[] dataBufferByte;
    private final short[] dataBufferShort;
    private final int[] dataBufferInt;
    private final float[] dataBufferFloat;
    private final double[] dataBufferDouble;

    private ProductData dataBuffer;
    private ProductData rawSamples;
    private boolean mustWriteSampleData;

    public TileImpl(RasterDataNode rasterDataNode, Raster raster) {
        this(rasterDataNode, raster, new Rectangle(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight()), false);
    }

    public TileImpl(RasterDataNode rasterDataNode, WritableRaster raster, Rectangle rectangle) {
        this(rasterDataNode, raster, rectangle, true);
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
        this.scalingApplied = rasterDataNode.isScalingApplied();
        this.target = target;

        int smX0 = rectangle.x - raster.getSampleModelTranslateX();
        int smY0 = rectangle.y - raster.getSampleModelTranslateY();
        int dbI0 = db.getOffset();
        this.scanlineStride = sm.getScanlineStride();
        this.scanlineOffset = smY0 * scanlineStride + smX0 + dbI0;

        Object primitiveArray = ImageUtils.getPrimitiveArray(db);
        this.dataBufferByte = (primitiveArray instanceof byte[]) ? (byte[]) primitiveArray : null;
        this.dataBufferShort = (primitiveArray instanceof short[]) ? (short[]) primitiveArray : null;
        this.dataBufferInt = (primitiveArray instanceof int[]) ? (int[]) primitiveArray : null;
        this.dataBufferFloat = (primitiveArray instanceof float[]) ? (float[]) primitiveArray : null;
        this.dataBufferDouble = (primitiveArray instanceof double[]) ? (double[]) primitiveArray : null;
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

    public final int getMaxX() {
        return maxX;
    }

    public final int getMinY() {
        return minY;
    }

    public final int getMaxY() {
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

    public int getDataBufferIndex(int x, int y) {
        return scanlineOffset + (x - minX) + (y - minY) * scanlineStride;
    }

    public synchronized ProductData getDataBuffer() {
        if (dataBuffer == null) {
            dataBuffer = ProductData.createInstance(rasterDataNode.getDataType(), ImageUtils.getPrimitiveArray(raster.getDataBuffer()));
        }
        return dataBuffer;
    }

    public final byte[] getDataBufferByte() {
        return dataBufferByte;
    }

    public final short[] getDataBufferShort() {
        return dataBufferShort;
    }

    public final int[] getDataBufferInt() {
        return dataBufferInt;
    }

    public final float[] getDataBufferFloat() {
        return dataBufferFloat;
    }

    public final double[] getDataBufferDouble() {
        return dataBufferDouble;
    }

    public final int getScanlineOffset() {
        return scanlineOffset;
    }

    public final int getScanlineStride() {
        return scanlineStride;
    }

    public synchronized ProductData getRawSamples() {
        if (rawSamples == null) {
            ProductData dataBuffer = getDataBuffer();
            if (width * height == dataBuffer.getNumElems()) {
                rawSamples = dataBuffer;
            }
        }
        if (rawSamples == null) {
            rawSamples = rasterDataNode.createCompatibleRasterData(width, height);
            if (target) {
                mustWriteSampleData = true;
            } else {
                raster.getDataElements(minX, minY, width, height, rawSamples.getElems());
            }
        }
        return rawSamples;
    }

    public synchronized void setRawSamples(ProductData rawSamples) {
        Assert.notNull(rawSamples, "rawSamples");
        if (target) {
            if (rawSamples != this.rawSamples || mustWriteSampleData) {
                writableRaster.setDataElements(minX, minY,
                                               width, height,
                                               rawSamples.getElems());
            }
        }
    }

    public boolean getSampleBoolean(int x, int y) {
        return getSampleInt(x, y) != 0;
    }

    public void setSample(int x, int y, boolean sample) {
        setSample(x, y, sample ? 1 : 0);
    }

    public int getSampleInt(int x, int y) {
        int sample = raster.getSample(x, y, 0);
        // todo - handle unsigned data types here!!!
        if (scalingApplied) {
            sample = (int) Math.floor(rasterDataNode.scale(sample) + 0.5);
        }
        return sample;
    }

    public void setSample(int x, int y, int sample) {
        if (scalingApplied) {
            sample = (int) Math.floor(rasterDataNode.scaleInverse(sample) + 0.5);
        }
        writableRaster.setSample(x, y, 0, sample);
    }

    public float getSampleFloat(int x, int y) {
        float sample = raster.getSampleFloat(x, y, 0);
        if (scalingApplied) {
            sample = (float) rasterDataNode.scale(sample);
        }
        return sample;
    }

    public void setSample(int x, int y, float sample) {
        if (scalingApplied) {
            sample = (float) rasterDataNode.scaleInverse(sample);
        }
        writableRaster.setSample(x, y, 0, sample);
    }

    public double getSampleDouble(int x, int y) {
        double sample = raster.getSampleDouble(x, y, 0);
        if (scalingApplied) {
            sample = rasterDataNode.scale(sample);
        }
        return sample;
    }

    public void setSample(int x, int y, double sample) {
        if (scalingApplied) {
            sample = rasterDataNode.scaleInverse(sample);
        }
        writableRaster.setSample(x, y, 0, sample);
    }
    
    public boolean getSampleBit(int x, int y, int bitIndex) {
        long sample = raster.getSample(x, y, 0);
        return BitSetter.isFlagSet(sample, bitIndex);
    }
    
    public void setSample(int x, int y, int bitIndex, boolean sample) {
        long longSample = raster.getSample(x, y, 0);
        long newSample = BitSetter.setFlag(longSample, bitIndex, sample);
        writableRaster.setSample(x, y, 0, newSample);
    }

    @Override
    public Iterator<Pos> iterator() {
        return new DefaultTileIterator(getRectangle());
    }
}
