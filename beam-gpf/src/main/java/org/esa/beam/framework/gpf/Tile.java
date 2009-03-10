package org.esa.beam.framework.gpf;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.Rectangle;
import java.util.Iterator;


/**
 * A tile represents a rectangular region of sample data within the scene rectangle of a data product.
 * Tiles are used to enable the sample data transfer from and to the source and target bands of data
 * products used within operator graphs.
 * <p>Target tiles to be computed are passed into an {@link org.esa.beam.framework.gpf.Operator Operator}'s
 * {@link Operator#computeTile(org.esa.beam.framework.datamodel.Band, Tile,com.bc.ceres.core.ProgressMonitor) computeTile}
 * and {@link Operator#computeTileStack(java.util.Map, java.awt.Rectangle, com.bc.ceres.core.ProgressMonitor) computeTileStack}  computeTileStack} methods.
 * Source tiles are obtained by using the
 * {@link Operator#getSourceTile(org.esa.beam.framework.datamodel.RasterDataNode,java.awt.Rectangle,com.bc.ceres.core.ProgressMonitor) getSourceTile} method.</p>
 * <p>Three ways are provided to access and manipulate the sample data of a target tile:</p>
 * <p>(1) This is the simplest but also slowest way to modify sample data of a tile:</p>
 * <pre>
 *   for (int y = tile.getMinY(); y &lt;= tile.getMaxY(); y++) {
 *       for (int x = tile.getMinX(); x &lt;= tile.getMaxX(); x++) {
 *           // compute sample value...
 *           tile.setSample(x, y, sample);
 *       }
 *   }
 * </pre>
 * which can also be written even simpler using a tile itertor:
 * <pre>
 *   for (Tile.Pos pos : tile) {
 *       // compute sample value...
 *       tile.setSample(pos.x, pos.y, sample);
 *   }
 * </pre>
 * <p/>
 * <p>(2) More performance is gained if the sample data buffer is checked out and committed
 * (required after modification only):</p>
 * <pre>
 *   ProductData samples = tile.getRawSamples(); // check out
 *   for (int y = 0; y &lt; tile.getHeight(); y++) {
 *       for (int x = 0; x &lt; tile.getWidth(); x++) {
 *           // compute sample value...
 *           samples.setElemFloatAt(y * getWidth() + x, sample);
 *           // ...
 *       }
 *   }
 *   tile.setRawSamples(samples); // commit
 * </pre>
 * <p>(3) The the fastest way to read from or write to sample data is to directly access the sample data
 * via their primitive data buffers:</p>
 * <pre>
 *   float[] samples = tile.getDataBufferFloat();
 *   float sample;
 *   int offset = tile.getScanlineOffset();
 *   for (int y = 0; y &lt; tile.getHeight(); y++) {
 *       int index = offset;
 *       for (int x = 0; x &lt; tile.getWidth(); x++) {
 *           // compute sample value...
 *           samples[index] = sample;
 *           index++;
 *       }
 *       offset += tile.getScanlineStride();
 *   }
 * </pre>
 * <p>Note that method (3) can only be used if the exact sample data type
 * is known or has been identified in a former step. The code snippet above
 * implies that the underlying data type is {@code float}
 * (because {@link org.esa.beam.framework.datamodel.RasterDataNode#getDataType() getRasterDataNode().getDataType()}
 * returns {@link ProductData#TYPE_FLOAT32}).</p>
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Marco Zühlke
 * @since 4.1
 */
public interface Tile extends Iterable<Tile.Pos> {

    /**
     * Gets the {@code RasterDataNode} associated with this tile,
     * e.g. a {@link org.esa.beam.framework.datamodel.Band Band} for source and target tiles or
     * {@link org.esa.beam.framework.datamodel.TiePointGrid TiePointGrid} for a source tile.
     *
     * @return The {@code RasterDataNode} associated with this tile.
     */
    RasterDataNode getRasterDataNode();

    /**
     * Checks if this is a target tile. Non-target tiles are read only.
     *
     * @return <code>true</code> if this is a target tile.
     */
    boolean isTarget();

    /**
     * Gets the tile rectangle in pixel coordinates within the scene covered by the tile's {@link #getRasterDataNode RasterDataNode}.
     * Simply returns <code>new Rectangle(
     * {@link #getMinX() minX},
     * {@link #getMinY() minY},
     * {@link #getWidth() width},
     * {@link #getHeight() height})</code>.
     *
     * @return The tile rectangle in pixel coordinates.
     */
    Rectangle getRectangle();

    /**
     * Gets the minimum pixel x-coordinate within the scene covered by the tile's {@link #getRasterDataNode RasterDataNode}.
     *
     * @return The minimum pixel x-coordinate.
     */
    int getMinX();

    /**
     * Gets the maximum pixel x-coordinate within the scene covered by the tile's {@link #getRasterDataNode RasterDataNode}.
     *
     * @return The maximum pixel x-coordinate.
     */
    int getMaxX();

    /**
     * Gets the minimum pixel y-coordinate within the scene covered by the tile's {@link #getRasterDataNode RasterDataNode}.
     *
     * @return The minimum pixel y-coordinate.
     */
    int getMinY();

    /**
     * Gets the maximum pixel y-coordinate within the scene covered by the tile's {@link #getRasterDataNode RasterDataNode}.
     *
     * @return The maximum pixel y-coordinate.
     */
    int getMaxY();

    /**
     * Gets the width in pixels within the scene covered by the tile's {@link #getRasterDataNode RasterDataNode}.
     *
     * @return The width in pixels.
     */
    int getWidth();

    /**
     * Gets the height in pixels within the scene covered by the tile's {@link RasterDataNode}.
     *
     * @return The height in pixels.
     */
    int getHeight();

    /**
     * Gets the index into the underlying data buffer for the given pixel coordinates.
     * <p>The pixel coordinates are absolute; meaning they are defined in the scene raster coordinate system
     * of this tile's {@link #getRasterDataNode()  RasterDataNode}.
     * </p>
     * <p>The returned index is computed as follows:
     * <pre>
     *   int dx = x - {@link #getMinX()};
     *   int dy = y - {@link #getMinY()};
     *   int index = {@link #getScanlineOffset()} + dy * {@link #getScanlineStride()} + dx;
     * </pre>
     * </p>
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     *
     * @return an index into the underlying data buffer
     */
    int getDataBufferIndex(int x, int y);

    /**
     * <p>Obtains access to the underlying data buffer. The data buffer holds the
     * raw (unscaled, uncalibrated) sample data (e.g. detector counts).</p>
     * Elements in this array must be addressed
     * by an index computed via the {@link #getScanlineStride() scanlineStride} and
     * {@link #getScanlineOffset() scanlineOffset} properties.
     * The index can also be directly computed using the  {@link #getDataBufferIndex(int,int)} method.
     * <p/>
     * <p>The most efficient way to access and/or modify the samples in the raw data buffer is using
     * the following nested loops:</p>
     * <pre>
     *   int lineStride = tile.{@link #getScanlineStride()};
     *   int lineOffset = tile.{@link #getScanlineOffset()};
     *   for (int y = tile.{@link #getMinY()}; y &lt;= tile.{@link #getMaxY()}; y++) {
     *      int index = lineOffset;
     *      for (int x = tile.{@link #getMinX()}; x &lt;= tile.{@link #getMaxX()}; x++) {
     *           // use index here to access raw data buffer...
     *           index++;
     *       }
     *       lineOffset += lineStride;
     *   }
     * </pre>
     * <p/>
     * <p>If the absolute x,y pixel coordinates are not required, the following construct may be a bit more
     * readable:</p>
     * <pre>
     *   int lineStride = tile.{@link #getScanlineStride()};
     *   int lineOffset = tile.{@link #getScanlineOffset()};
     *   for (int y = 0; y &lt; tile.{@link #getHeight()}; y++) {
     *      int index = lineOffset;
     *      for (int x = 0; x &lt; tile.{@link #getWidth()}; x++) {
     *           // use index here to access raw data buffer...
     *           index++;
     *       }
     *       lineOffset += lineStride;
     *   }
     * </pre>
     *
     * @return the sample data
     */
    ProductData getDataBuffer();

    /**
     * Gets the underlying data buffer's primitive array of type <code>byte</code> (signed or unsigned).
     * If the underlying data buffer is not of type <code>byte</code>, <code>null</code> is returned.
     * <p>Refer to {@link #getDataBuffer()} for using the primitive array.</p>
     *
     * @return The underlying data buffer's primitive array, or <code>null</code> (see above).
     *
     * @see #getDataBufferIndex(int,int)
     * @see #getDataBuffer()
     */
    byte[] getDataBufferByte();

    /**
     * Gets the underlying data buffer's primitive array of type <code>short</code> (signed or unsigned).
     * If the underlying data buffer is not of type <code>short</code>, <code>null</code> is returned.
     * <p>Refer to {@link #getDataBuffer()} for using the primitive array.</p>
     *
     * @return The underlying data buffer's primitive array, or <code>null</code> (see above).
     *
     * @see #getDataBufferIndex(int,int)
     * @see #getDataBuffer()
     */
    short[] getDataBufferShort();

    /**
     * Gets the underlying data buffer's primitive array of type <code>int</code>.
     * If the underlying data buffer is not of type <code>int</code>, <code>null</code> is returned.
     * <p>Refer to {@link #getDataBuffer()} for using the primitive array.</p>
     *
     * @return The underlying data buffer's primitive array, or <code>null</code> (see above).
     *
     * @see #getDataBufferIndex(int,int)
     * @see #getDataBuffer()
     */
    int[] getDataBufferInt();

    /**
     * Gets the underlying data buffer's primitive array of type <code>float</code>.
     * If the underlying data buffer is not of type <code>float</code>, <code>null</code> is returned.
     * <p>Refer to {@link #getDataBuffer()} for using the primitive array.</p>
     *
     * @return The underlying data buffer's primitive array, or <code>null</code> (see above).
     *
     * @see #getDataBufferIndex(int,int)
     * @see #getDataBuffer()
     */
    float[] getDataBufferFloat();

    /**
     * Gets the underlying data buffer's primitive array of type <code>double</code>.
     * If the underlying data buffer is not of type <code>double</code>, <code>null</code> is returned.
     * <p>Refer to {@link #getDataBuffer()} for using the primitive array.</p>
     *
     * @return The underlying data buffer's primitive array, or <code>null</code> (see above).
     *
     * @see #getDataBufferIndex(int,int)
     * @see #getDataBuffer()
     */
    double[] getDataBufferDouble();

    /**
     * Gets the scanline offset.
     * The scanline offset is the index to the first valid sample element in the data buffer.
     *
     * @return The raster scanline offset.
     *
     * @see #getScanlineStride()
     */
    int getScanlineOffset();

    /**
     * Gets the raster scanline stride for addressing the internal data buffer.
     * The scanline stride is added to the scanline offset in order to compute offsets of subsequent scanlines.
     *
     * @return The raster scanline stride.
     *
     * @see #getScanlineOffset()
     */
    int getScanlineStride();

    /**
     * Gets raw (unscaled, uncalibrated) samples, e.g. detector counts, copied from or wrapping the underlying
     * data buffer. In contradiction to the {@link #getDataBuffer()} method, the returned samples
     * will cover exactly the region {@link #getRectangle()} rectangle} of this tile. Thus, the number
     * of returned samples will always equal {@link #getWidth() width} {@code *} {@link #getHeight() height}.
     * <p>In order to apply changes of the samples values to this tile, it is mandatory to call
     * {@link #setRawSamples(org.esa.beam.framework.datamodel.ProductData)} with the modified
     * {@code ProductData} instance.</p>
     *
     * @return The raw samples copied from or wrapping the underlying data buffer.
     */
    ProductData getRawSamples();

    /**
     * Sets raw (unscaled, uncalibrated) samples, e.g. detector counts, into the underlying.
     * The number of given samples must be equal {@link #getWidth() width} {@code *} {@link #getHeight() height}
     * of this tile.
     * <p>This method must be used
     * in order to apply changes made to the samples returned by the {@link #getRawSamples()} method.</p>
     *
     * @param samples The raw samples to be set.
     */
    void setRawSamples(ProductData samples);

// todo - define getSamples():ProductData (nf - 09.10.2007)
// todo - define setSamples(samples:ProductData):void (nf - 09.10.2007)

    /**
     * Gets the geophysical (scaled, calibrated) sample at the given pixel coordinate as {@code boolean} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     *
     * @return The geophysical sample as {@code boolean} value.
     */
    boolean getSampleBoolean(int x, int y);

    /**
     * Sets the geophysical (scaled, calibrated) sample at the given pixel coordinate from a {@code boolean} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x      The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y      The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param sample The geophysical sample as {@code boolean} value.
     */
    void setSample(int x, int y, boolean sample);

    /**
     * Gets the geophysical (scaled, calibrated) sample at the given pixel coordinate as {@code int} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     *
     * @return The geophysical sample as {@code int} value.
     */
    int getSampleInt(int x, int y);

    /**
     * Sets the geophysical (scaled, calibrated) sample at the given pixel coordinate from a {@code int} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x      The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y      The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param sample The geophysical sample as {@code int} value.
     */
    void setSample(int x, int y, int sample);

    /**
     * Gets the geophysical (scaled, calibrated) sample at the given pixel coordinate as {@code float} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     *
     * @return The geophysical sample as {@code float} value.
     */
    float getSampleFloat(int x, int y);

    /**
     * Sets the geophysical (scaled, calibrated) sample at the given pixel coordinate from a {@code float} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x      The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y      The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param sample The geophysical sample as {@code float} value.
     */
    void setSample(int x, int y, float sample);

    /**
     * Gets the geophysical (scaled, calibrated) sample value for the given pixel coordinate as {@code double}.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     *
     * @return The geophysical sample as {@code double} value.
     */
    double getSampleDouble(int x, int y);

    /**
     * Sets the geophysical (scaled, calibrated) sample at the given pixel coordinate from a {@code double} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x      The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y      The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param sample The geophysical sample as {@code double} value.
     */
    void setSample(int x, int y, double sample);

    /**
     * Gets the sample value for the given pixel coordinate and the specified bit index as {@code boolean}.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x        The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y        The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param bitIndex The bit index.
     *
     * @return The sample as {@code boolean} value.
     */
    boolean getSampleBit(int x, int y, int bitIndex);

    /**
     * Sets the sample at the given pixel coordinate and the specified bit index from a {@code boolean} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x        The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y        The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param bitIndex The bit index.
     * @param sample   The sample as {@code boolean} value.
     */
    void setSample(int x, int y, int bitIndex, boolean sample);

    /**
     * Gets an iterator which can be used to visit all pixels in the tile.
     * The method allows this tile to be the target of the Java "foreach" statement.
     * Using the tile as an iterator in a single loop
     * <pre>
     * for (Tile.Pos pos: tile) {
     *    int x = pos.x;
     *    int y = pos.y;
     *    // ...
     * }
     * </pre>
     * is equivalent to iterating over all pixels using two nested loops
     * <pre>
     * for (int y = tile.getMinY(); y <= tile.getMaxY(); y++) {
     *     for (int x = tile.getMinX(); x <= tile.getMaxX(); x++) {
     *         // ...
     *     }
     * }
     * </pre>
     */
    @Override
    Iterator<Pos> iterator();

    /**
     * A pixel position within the tile's raster.
     */
    public final class Pos {

        public final int x;
        public final int y;

        public Pos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Pos) {
                Pos pos = (Pos) obj;
                return pos.x == x && pos.y == y;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return x - y;
        }

        @Override
        public String toString() {
            return getClass().getName() + "[x=" + x + ",y=" + y + "]";
        }
    }
}
