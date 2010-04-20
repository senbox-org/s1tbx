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
    private final boolean raw;
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
        this.target = target;
        // todo - optimize getSample()/setSample() methods by using a Closure that either honours calibration or not. (nf 04.2010)
        this.raw = rasterDataNode.isScalingApplied();

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

    @Override
    public float toGeoPhysical(float sample) {
        return (float) rasterDataNode.scale(sample);
    }

    @Override
    public double toGeoPhysical(double sample) {
        return rasterDataNode.scale(sample);
    }

    @Override
    public float toRaw(float sample) {
        return (float) rasterDataNode.scaleInverse(sample);
    }

    @Override
    public double toRaw(double sample) {
        return rasterDataNode.scaleInverse(sample);
    }

    @Override
    public final boolean isTarget() {
        return target;
    }

    @Override
    public boolean isSampleValid(int x, int y) {
        // todo - THIS IS VERY INEFFICIENT! (nf - 04.2010) 
        // fixme - read flag directly from a validMaskTile:TileImpl (nf - 04.2010)
        return rasterDataNode.isPixelValid(x, y);
    }

    @Override
    public final Rectangle getRectangle() {
        return new Rectangle(minX, minY, width, height);
    }

    @Override
    public final int getMinX() {
        return minX;
    }

    @Override
    public final int getMaxX() {
        return maxX;
    }

    @Override
    public final int getMinY() {
        return minY;
    }

    @Override
    public final int getMaxY() {
        return maxY;
    }

    @Override
    public final int getWidth() {
        return width;
    }

    @Override
    public final int getHeight() {
        return height;
    }

    @Override
    public final RasterDataNode getRasterDataNode() {
        return rasterDataNode;
    }

    @Override
    public int getDataBufferIndex(int x, int y) {
        return scanlineOffset + (x - minX) + (y - minY) * scanlineStride;
    }

    @Override
    public synchronized ProductData getDataBuffer() {
        if (dataBuffer == null) {
            dataBuffer = ProductData.createInstance(rasterDataNode.getDataType(), ImageUtils.getPrimitiveArray(raster.getDataBuffer()));
        }
        return dataBuffer;
    }

    @Override
    public final byte[] getDataBufferByte() {
        return dataBufferByte;
    }

    @Override
    public final short[] getDataBufferShort() {
        return dataBufferShort;
    }

    @Override
    public final int[] getDataBufferInt() {
        return dataBufferInt;
    }

    @Override
    public final float[] getDataBufferFloat() {
        return dataBufferFloat;
    }

    @Override
    public final double[] getDataBufferDouble() {
        return dataBufferDouble;
    }

    @Override
    public final int getScanlineOffset() {
        return scanlineOffset;
    }

    @Override
    public final int getScanlineStride() {
        return scanlineStride;
    }

    @Override
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

    @Override
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

    @Override
    public float[] getSamplesFloat() {
        final ProductData data = getRawSamples();
        if (raw) {
            final int size = data.getNumElems();
            float[] samples = new float[size];
            for (int i = 0; i < size; i++) {
                samples[i] = toGeoPhysical(data.getElemFloatAt(i));
            }
            return samples;
        } else {
            if (data.getType() == ProductData.TYPE_FLOAT32) {
                return (float[]) data.getElems();
            } else {
                final int size = data.getNumElems();
                float[] samples = new float[size];
                for (int i = 0; i < size; i++) {
                    samples[i] = data.getElemFloatAt(i);
                }
                return samples;
            }
        }
    }

    @Override
    public double[] getSamplesDouble() {
        final ProductData data = getRawSamples();
        if (raw) {
            final int size = data.getNumElems();
            double[] samples = new double[size];
            for (int i = 0; i < size; i++) {
                samples[i] = toGeoPhysical(data.getElemDoubleAt(i));
            }
            return samples;
        } else {
            if (data.getType() == ProductData.TYPE_FLOAT64) {
                return (double[]) data.getElems();
            } else {
                final int size = data.getNumElems();
                double[] samples = new double[size];
                for (int i = 0; i < size; i++) {
                    samples[i] = data.getElemDoubleAt(i);
                }
                return samples;
            }
        }
    }

    @Override
    public void setSamples(float[] samples) {
        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                setSample(x, y, samples[i++]);
            }
        }
    }

    @Override
    public void setSamples(double[] samples) {
        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                setSample(x, y, samples[i++]);
            }
        }
    }

    @Override
    public boolean getSampleBoolean(int x, int y) {
        return getSampleInt(x, y) != 0;
    }

    @Override
    public void setSample(int x, int y, boolean sample) {
        setSample(x, y, sample ? 1 : 0);
    }

    @Override
    public int getSampleInt(int x, int y) {
        int sample = raster.getSample(x, y, 0);
        // todo - handle unsigned data types here!!!
        if (raw) {
            sample = (int) Math.floor(toGeoPhysical(sample) + 0.5);
        }
        return sample;
    }

    @Override
    public void setSample(int x, int y, int sample) {
        // todo - handle unsigned data types here!!!
        if (raw) {
            sample = (int) Math.floor(toRaw((double) sample) + 0.5);
        }
        writableRaster.setSample(x, y, 0, sample);
    }

    @Override
    public float getSampleFloat(int x, int y) {
        float sample = raster.getSampleFloat(x, y, 0);
        if (raw) {
            sample = toGeoPhysical(sample);
        }
        return sample;
    }

    @Override
    public void setSample(int x, int y, float sample) {
        if (raw) {
            sample = toRaw(sample);
        }
        writableRaster.setSample(x, y, 0, sample);
    }


    @Override
    public double getSampleDouble(int x, int y) {
        double sample = raster.getSampleDouble(x, y, 0);
        if (raw) {
            sample = toGeoPhysical(sample);
        }
        return sample;
    }

    @Override
    public void setSample(int x, int y, double sample) {
        if (raw) {
            sample = toRaw(sample);
        }
        writableRaster.setSample(x, y, 0, sample);
    }

    @Override
    public boolean getSampleBit(int x, int y, int bitIndex) {
        long sample = raster.getSample(x, y, 0);
        return BitSetter.isFlagSet(sample, bitIndex);
    }
    
    @Override
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
