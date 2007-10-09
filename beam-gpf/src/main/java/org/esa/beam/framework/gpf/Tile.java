package org.esa.beam.framework.gpf;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.Rectangle;

// todo (nf, 26.09.2007) - API revision:
// - rename class to Tile
// - IMPORTANT: If we enable this tile to pass out references to primitive arrays of
//   the AWT raster's internal data buffer, we also have to provide
//   getScanlineOffset() and getScanlineStride()
//   methods. Note that these are totally different from getOffsetX() and getOffsetY()
//   which are defined within the image's pixel coodinate systems while the former are
//   defined only for the primitive array references passed out.
//
//   <p>This is the simplest but also slowest way to modify sample data of a tile:</p>
//   <pre>
//     for (int y = 0; y < getHeight(); y++) {
//         for (int x = 0; x < getWidth(); x++) {
//             // compute sample value...
//             tile.setSample(x, y, sample);
//         }
//     }
//   </pre>
//
//   <p>More performance is gained if the sample data buffer is checked out and committed
//   (required after modification only):</p>
//
//   <pre>
//     ProductData samples = tile.getRawSampleData(); // check out
//     for (int y = 0; y < getHeight(); y++) {
//         for (int x = 0; x < getWidth(); x++) {
//             // compute sample value...
//             samples.setElemFloatAt(y * getWidth() + x, sample);
//             // ...
//         }
//     }
//     tile.setRawSampleData(samples); // commit
//   </pre>
//
//   <p>The the fastest way to read from or write to sample data is via the primitive sample arrays:</p>
//
//   <pre>
//     float[] samples = tile.getRawSamplesFloat();
//     float sample;
//     int offset = tile.getScanlineOffset();
//     for (int y = 0; y < getHeight(); y++) {
//         int index = offset;
//         for (int x = 0; x < getWidth(); x++) {
//             // compute sample value...
//             samples[index] = sample;
//             index++;
//         }
//         offset += tile.getScanlineStride();
//     }
//   </pre>
//
//   Currently, we pass out a primitive reference only if the array.length equals
//   the area of the getRectangle(), which is never the case if the requested rectangle is
//   not equal to the tile rectangle.
//

/**
 * A tile.
 */
public interface Tile {

    /**
     * Gets the {@code RasterDataNode} associated with this tile,
     * e.g. a {@link org.esa.beam.framework.datamodel.Band Band} or
     * {@link org.esa.beam.framework.datamodel.TiePointGrid TiePointGrid}.
     *
     * @return The {@code RasterDataNode} associated with this tile.
     */
    RasterDataNode getRasterDataNode();

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
     * @see #getDataBufferIndex(int,int)
     * @see #getDataBuffer()
     */
    double[] getDataBufferDouble();

    /**
     * Gets the scanline offset.
     * The scanline offset is the index to the first valid sample element in the data buffer.
     *
     * @return The raster scanline offset.
     * @see #getScanlineStride()
     */
    int getScanlineOffset();

    /**
     * Gets the raster scanline stride for addressing the internal data buffer.
     * The scanline stride is added to the scanline offset in order to compute offsets of subsequent scanlines.
     *
     * @return The raster scanline stride.
     * @see #getScanlineOffset()
     */
    int getScanlineStride();

// todo - rename the following accessor pair
//    ProductData getRawSamples();

    //    void setRawSamples(ProductData samples);

    ProductData getRawSampleData();

    void setRawSampleData(ProductData sampleData);
// todo - this is the scaled equivalent accessor pair. We should provide this !
//    ProductData getSamples();
//    void setSamples(ProductData samples);

    /**
     * Gets a sample value as {@code boolean} for the given pixel coordinate.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @return A sample value as {@code boolean}.
     */
    boolean getSampleBoolean(int x, int y);

    void setSample(int x, int y, boolean sample);

    /**
     * Gets a sample value as {@code int} for the given pixel coordinate.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @return A sample value as {@code int}.
     */
    int getSampleInt(int x, int y);

    void setSample(int x, int y, int sample);

    /**
     * Gets a sample value as {@code float} for the given pixel coordinate.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @return A sample value as {@code float}.
     */
    float getSampleFloat(int x, int y);

    void setSample(int x, int y, float sample);

    /**
     * Gets a sample value as {@code double} for the given pixel coordinate.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.</p>
     * <p>Note that it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.</p>
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @return A sample value as {@code double}.
     */
    double getSampleDouble(int x, int y);

    void setSample(int x, int y, double sample);

    /**
     * Checks if this is a target tile. Non-target tiles are read only.
     *
     * @return <code>true</code> if this is a target tile.
     */
    boolean isTarget();

}
