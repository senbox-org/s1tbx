package org.esa.snap.core.gpf;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.internal.DefaultTileIterator;
import org.esa.snap.core.util.BitSetter;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.util.Iterator;


/**
 * This test is used to simulate a different GPF driver ("tile computation requester").
 *
 * @author Norman
 */
public class DirectDriverTest extends TestCase {

    static final int W = 4;
    static final int H = 4;
    static final float valueA = 0.4f;
    static final float valueB = 0.5f;

    public void testDirect() {
        Operator op = new MyOp();
        Product sourceProduct = new Product("N", "T", W, H);
        sourceProduct.addBand("a", ProductData.TYPE_FLOAT32).setSourceImage(ConstantDescriptor.create((float) W, (float) H, new Float[]{valueA}, null));
        sourceProduct.addBand("b", ProductData.TYPE_FLOAT32).setSourceImage(ConstantDescriptor.create((float) W, (float) H, new Float[]{valueB}, null));
        op.setSourceProduct(sourceProduct);
        Product targetProduct = op.getTargetProduct();
        Band bandC = targetProduct.getBand("c");

        ProductData data = bandC.createCompatibleRasterData(W, 1);
        DirectTile row1 = new DirectTile(bandC, data, new Rectangle(0, 0, W, 1));
        op.computeTile(bandC, row1, ProgressMonitor.NULL);

        assertEquals(valueA + valueB, data.getElemFloatAt(0));
        assertEquals(valueA + valueB, data.getElemFloatAt(1));
        assertEquals(valueA + valueB, data.getElemFloatAt(2));
        assertEquals(valueA + valueB, data.getElemFloatAt(3));
    }

    public static class MyOp extends Operator {
        @SourceProduct
        Product sourceProduct;
        @TargetProduct
        Product targetProduct;

        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product("N", "T", sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            targetProduct.addBand("c", ProductData.TYPE_FLOAT32);
        }

        @Override
        public void computeTile(Band targetBand, Tile tileC, ProgressMonitor pm) throws OperatorException {
            Tile tileA = getSourceTile(getSourceProduct().getBand("a"), tileC.getRectangle());
            Tile tileB = getSourceTile(getSourceProduct().getBand("b"), tileC.getRectangle());
            for (Tile.Pos pos : tileC) {
                float a = tileA.getSampleFloat(pos.x, pos.y);
                float b = tileB.getSampleFloat(pos.x, pos.y);
                float c = a + b;
                tileC.setSample(pos.x, pos.y, c);
            }
        }
    }

    /**
     * Note: most of this code is copied from TileImpl.
     * The class still shares most of its impl. code. (Try IDEA's compare with clipboard) 
     */
    private static class DirectTile implements Tile {
        private final RasterDataNode rasterDataNode;
        private ProductData dataBuffer;
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;
        private final int width;
        private final int height;
        private final boolean target;
        private final boolean scaled;
        private final boolean signedByte;

        private final byte[] dataBufferByte;
        private final short[] dataBufferShort;
        private final int[] dataBufferInt;
        private final float[] dataBufferFloat;
        private final double[] dataBufferDouble;

        public DirectTile(RasterDataNode rasterDataNode, ProductData dataBuffer, Rectangle rectangle) {
            this(rasterDataNode, dataBuffer, rectangle, true);
        }

        public DirectTile(RasterDataNode rasterDataNode, ProductData dataBuffer, Rectangle rectangle, boolean target) {
            Assert.notNull(rasterDataNode, "rasterDataNode");

            this.rasterDataNode = rasterDataNode;
            this.dataBuffer = dataBuffer;
            this.minX = rectangle.x;
            this.minY = rectangle.y;
            this.maxX = rectangle.x + rectangle.width - 1;
            this.maxY = rectangle.y + rectangle.height - 1;
            this.width = rectangle.width;
            this.height = rectangle.height;
            this.target = target;
            this.scaled = rasterDataNode.isScalingApplied();
            this.signedByte = rasterDataNode.getDataType() == ProductData.TYPE_INT8;

            this.dataBufferByte = (dataBuffer instanceof ProductData.Byte) ? ((ProductData.Byte) dataBuffer).getArray() : null;
            this.dataBufferShort = (dataBuffer instanceof ProductData.Short) ? ((ProductData.Short) dataBuffer).getArray() : null;
            this.dataBufferInt = (dataBuffer instanceof ProductData.Int) ? ((ProductData.Int) dataBuffer).getArray() : null;
            this.dataBufferFloat = (dataBuffer instanceof ProductData.Float) ? ((ProductData.Float) dataBuffer).getArray() : null;
            this.dataBufferDouble = (dataBuffer instanceof ProductData.Double) ? ((ProductData.Double) dataBuffer).getArray() : null;
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
            return (y - minY) * width + x - minX;
        }

        @Override
        public synchronized ProductData getDataBuffer() {
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
            return 0;
        }

        @Override
        public final int getScanlineStride() {
            return width;
        }

        @Override
        public synchronized ProductData getRawSamples() {
            return dataBuffer;
        }

        @Override
        public synchronized void setRawSamples(ProductData rawSamples) {
            if (target) {
                if (rawSamples != this.dataBuffer) {
                    Assert.notNull(rawSamples, "rawSamples");
                    Assert.argument(rawSamples.getType() == dataBuffer.getType(), "rawSamples.getType() == dataBuffer.getType()");
                    Assert.argument(rawSamples.getNumElems() == dataBuffer.getNumElems(), "rawSamples.getNumElems() == dataBuffer.getNumElems()");
                    dataBuffer.setElems(rawSamples.getElems());
                }
            }
        }

        @Override
        public byte[] getSamplesByte() {
            if (getRasterDataNode().isValidMaskUsed()) {
                final int size = width * height;
                final byte[] samples = new byte[size];
                int i = 0;
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        samples[i++] = isSampleValid(x, y) ? (byte)getSampleInt(x, y) : 0;
                    }
                }
                return samples;
            } else {
                final ProductData data = getRawSamples();
                if (!scaled && (data.getType() == ProductData.TYPE_INT8 || data.getType() == ProductData.TYPE_UINT8)) {
                    return (byte[]) data.getElems();
                }
                final int size = data.getNumElems();
                final byte[] samples = new byte[size];
                if (scaled) {
                    for (int i = 0; i < size; i++) {
                        samples[i] = (byte) toGeoPhysical(data.getElemIntAt(i));
                    }
                } else {
                    for (int i = 0; i < size; i++) {
                        samples[i] = (byte)data.getElemIntAt(i);
                    }
                }
                return samples;
            }
        }

        @Override
        public short[] getSamplesShort() {
            if (getRasterDataNode().isValidMaskUsed()) {
                final int size = width * height;
                final short[] samples = new short[size];
                int i = 0;
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        samples[i++] = isSampleValid(x, y) ? (short)getSampleInt(x, y) : 0;
                    }
                }
                return samples;
            } else {
                final ProductData data = getRawSamples();
                if (!scaled && (data.getType() == ProductData.TYPE_INT16 || data.getType() == ProductData.TYPE_UINT16)) {
                    return (short[]) data.getElems();
                }
                final int size = data.getNumElems();
                final short[] samples = new short[size];
                if (scaled) {
                    for (int i = 0; i < size; i++) {
                        samples[i] = (short) toGeoPhysical(data.getElemIntAt(i));
                    }
                } else {
                    for (int i = 0; i < size; i++) {
                        samples[i] = (short) data.getElemIntAt(i);
                    }
                }
                return samples;
            }
        }

        @Override
        public int[] getSamplesInt() {
            if (getRasterDataNode().isValidMaskUsed()) {
                final int size = width * height;
                final int[] samples = new int[size];
                int i = 0;
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        samples[i++] = isSampleValid(x, y) ? getSampleInt(x, y) : 0;
                    }
                }
                return samples;
            } else {
                final ProductData data = getRawSamples();
                if (!scaled && (data.getType() == ProductData.TYPE_INT32 || data.getType() == ProductData.TYPE_UINT32)) {
                    return (int[]) data.getElems();
                }
                final int size = data.getNumElems();
                final int[] samples = new int[size];
                if (scaled) {
                    for (int i = 0; i < size; i++) {
                        samples[i] = (int) toGeoPhysical(data.getElemIntAt(i));
                    }
                } else {
                    for (int i = 0; i < size; i++) {
                        samples[i] = data.getElemIntAt(i);
                    }
                }
                return samples;
            }
        }

        @Override
        public float[] getSamplesFloat() {
            if (getRasterDataNode().isValidMaskUsed()) {
                final int size = width * height;
                final float[] samples = new float[size];
                int i = 0;
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        samples[i++] = isSampleValid(x, y) ? getSampleFloat(x, y) : Float.NaN;
                    }
                }
                return samples;
            } else {
                final ProductData data = getRawSamples();
                if (!scaled && data.getType() == ProductData.TYPE_FLOAT32) {
                    return (float[]) data.getElems();
                }
                final int size = data.getNumElems();
                final float[] samples = new float[size];
                if (scaled) {
                    for (int i = 0; i < size; i++) {
                        samples[i] = toGeoPhysical(data.getElemFloatAt(i));
                    }
                } else {
                    for (int i = 0; i < size; i++) {
                        samples[i] = data.getElemFloatAt(i);
                    }
                }
                return samples;
            }
        }

        @Override
        public double[] getSamplesDouble() {
            if (getRasterDataNode().isValidMaskUsed()) {
                final int size = width * height;
                final double[] samples = new double[size];
                int i = 0;
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        samples[i++] = isSampleValid(x, y) ? getSampleDouble(x, y) : Double.NaN;
                    }
                }
                return samples;
            } else {
                final ProductData data = getRawSamples();
                if (!scaled && data.getType() == ProductData.TYPE_FLOAT32) {
                    return (double[]) data.getElems();
                }
                final int size = data.getNumElems();
                final double[] samples = new double[size];
                if (scaled) {
                    for (int i = 0; i < size; i++) {
                        samples[i] = toGeoPhysical(data.getElemDoubleAt(i));
                    }
                } else {
                    for (int i = 0; i < size; i++) {
                        samples[i] = data.getElemDoubleAt(i);
                    }
                }
                return samples;
            }
        }

        @Override
        public void setSamples(byte[] samples) {
            int i = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    setSample(x, y, samples[i++]);
                }
            }
        }

        @Override
        public void setSamples(short[] samples) {
            int i = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    setSample(x, y, samples[i++]);
                }
            }
        }

        @Override
        public void setSamples(int[] samples) {
            int i = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    setSample(x, y, samples[i++]);
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
            int sample = dataBuffer.getElemIntAt(index(x, y));
            // handle unsigned data types, see also [BEAM-1147] (nf - 20100527)
            if (signedByte) {
                //noinspection SillyAssignment
                sample = (byte) sample;
            }
            if (scaled) {
                sample = (int) Math.floor(toGeoPhysical(sample) + 0.5);
            }
            return sample;
        }

        @Override
        public void setSample(int x, int y, int sample) {
            if (scaled) {
                sample = (int) Math.floor(toRaw((double) sample) + 0.5);
            }
            dataBuffer.setElemIntAt(index(x, y), sample);
        }

        @Override
        public float getSampleFloat(int x, int y) {
            float sample = dataBuffer.getElemFloatAt(index(x, y));
            // handle unsigned data types, see also [BEAM-1147] (nf - 20100527)
            if (signedByte) {
                //noinspection SillyAssignment
                sample = (byte) sample;
            }
            if (scaled) {
                sample = toGeoPhysical(sample);
            }
            return sample;
        }

        @Override
        public void setSample(int x, int y, float sample) {
            if (scaled) {
                sample = toRaw(sample);
            }
            dataBuffer.setElemFloatAt(index(x, y), sample);
        }


        @Override
        public double getSampleDouble(int x, int y) {
            double sample = dataBuffer.getElemDoubleAt(index(x, y));
            // handle unsigned data types, see also [BEAM-1147] (nf - 20100527)
            if (signedByte) {
                //noinspection SillyAssignment
                sample = (byte) sample;
            }
            if (scaled) {
                sample = toGeoPhysical(sample);
            }
            return sample;
        }

        @Override
        public void setSample(int x, int y, double sample) {
            if (scaled) {
                sample = toRaw(sample);
            }
            dataBuffer.setElemDoubleAt(index(x, y), sample);
        }

        @Override
        public boolean getSampleBit(int x, int y, int bitIndex) {
            long sample = dataBuffer.getElemUIntAt(index(x, y));
            return BitSetter.isFlagSet(sample, bitIndex);
        }

        @Override
        public void setSample(int x, int y, int bitIndex, boolean sample) {
            long longSample = dataBuffer.getElemUIntAt(index(x, y));
            long newSample = BitSetter.setFlag(longSample, bitIndex, sample);
            dataBuffer.setElemUIntAt(index(x, y), newSample);
        }

        @Override
        public Iterator<Pos> iterator() {
            return new DefaultTileIterator(getRectangle());
        }

        private int index(int x, int y) {
            return (y - minY) * (maxY - minY + 1) + x - minX;
        }

    }

}
