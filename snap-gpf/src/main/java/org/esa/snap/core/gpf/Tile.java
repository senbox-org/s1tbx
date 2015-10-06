/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;

import java.awt.Rectangle;
import java.util.Iterator;


/**
 * A tile represents a rectangular region of sample data within the scene rectangle of a data product.
 * Tiles are used to enable the sample data transfer from and to the source and target bands of data
 * products used within operator graphs.
 * <p>Target tiles to be computed are passed into an {@link Operator}'s
 * {@link Operator#computeTile(Band, Tile, com.bc.ceres.core.ProgressMonitor) computeTile}
 * and {@link Operator#computeTileStack(java.util.Map, java.awt.Rectangle, com.bc.ceres.core.ProgressMonitor) computeTileStack}  computeTileStack} methods.
 * Source tiles are obtained by using the
 * {@link Operator#getSourceTile(RasterDataNode, java.awt.Rectangle) getSourceTile} method.
 * <p>Three ways are provided to access and manipulate the sample data of a target tile:
 * <p>(1) This is the simplest (but also slowest) way to modify sample data of a tile:
 * <pre>
 *   for (int y = tile.getMinY(); y &lt;= tile.getMaxY(); y++) {
 *       for (int x = tile.getMinX(); x &lt;= tile.getMaxX(); x++) {
 *           // compute sample value...
 *           tile.setSample(x, y, sample);
 *       }
 *   }
 * </pre>
 * which can also be written even simpler using a tile iterator:
 * <pre>
 *   for (Tile.Pos pos : tile) {
 *       // compute sample value...
 *       tile.setSample(pos.x, pos.y, sample);
 *   }
 * </pre>
 * <p>The methods {@link #getSampleFloat(int, int)} and {@link #setSample(int, int, float)} and their derivatives
 * used in option (1) return and expect (geo-)physically scaled sample values.
 * <p>(2) More performance is gained if the sample data buffer is checked out and committed
 * (required after modification only):
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
 * <p>The method {@link #getRawSamples()} used in option (2) returns a writable buffer for the raw, non-calibrated
 * sample values.
 * Use the {@link #toGeoPhysical(float)} and {@link #toRaw(float)} to convert between physical and raw
 * sample values.
 * <p>(3) The the fastest way to read from or write to sample data is to directly access the sample data
 * via their primitive data buffers:
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
 * <p>Note that option (3) can only be used if the exact sample data type
 * is known or has been identified in a former step. The code snippet above
 * implies that the underlying data type is {@code float}
 * (because {@link RasterDataNode#getDataType() getRasterDataNode().getDataType()}
 * returns {@link ProductData#TYPE_FLOAT32}).
 * The {@link #getDataBufferFloat()} and its derivatives all return arrays of raw, non-calibrated sample values.
 * Use the {@link #toGeoPhysical(float)} and {@link #toRaw(float)} to convert between physical and raw
 * sample values.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Marco Zühlke
 * @since 4.1
 */
public interface Tile extends Iterable<Tile.Pos> {

    /**
     * Gets the {@code RasterDataNode} associated with this tile,
     * e.g. a {@link Band Band} for source and target tiles or
     * {@link TiePointGrid TiePointGrid} for a source tile.
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
     * Converts a raw sample value (e.g. digital counts) to a (geo-)physically scaled sample value
     * of type {@code float}.
     *
     * @param rawSample The raw sample value.
     *
     * @return The calibrated sample value.
     */
    float toGeoPhysical(float rawSample);

    /**
     * Converts a raw sample value (e.g. digital counts) to a (geo-)physically scaled sample value
     * of type {@code double}.
     *
     * @param rawSample The raw sample value.
     *
     * @return The calibrated sample value.
     */
    double toGeoPhysical(double rawSample);

    /**
     * Converts a (geo-)physically scaled sample value of type {@code float} to a
     * its corresponding raw sample value (e.g. digital counts).
     *
     * @param sample The calibrated sample value.
     *
     * @return The raw sample value.
     */
    float toRaw(float sample);

    /**
     * Converts a (geo-)physically scaled sample value of type {@code double} to a
     * its corresponding raw sample value (e.g. digital counts).
     *
     * @param sample The calibrated sample value.
     *
     * @return The raw sample value.
     */
    double toRaw(double sample);

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
     * Gets the index into the underlying raw sample buffer for the given pixel coordinates.
     * <p>The pixel coordinates are absolute; meaning they are defined in the scene raster coordinate system
     * of this tile's {@link #getRasterDataNode()  RasterDataNode}.
     *
     * <p>The returned index is computed as follows:
     * <pre>
     *   int dx = x - {@link #getMinX()};
     *   int dy = y - {@link #getMinY()};
     *   int index = {@link #getScanlineOffset()} + dy * {@link #getScanlineStride()} + dx;
     * </pre>
     *
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     *
     * @return an index into the underlying data buffer
     */
    int getDataBufferIndex(int x, int y);

    /**
     * <p>Obtains access to the underlying raw sample buffer. The data buffer holds the
     * raw (unscaled, uncalibrated) sample data (e.g. detector counts).
     * Elements in this array must be addressed
     * by an index computed via the {@link #getScanlineStride() scanlineStride} and
     * {@link #getScanlineOffset() scanlineOffset} properties.
     * The index can also be directly computed using the  {@link #getDataBufferIndex(int, int)} method.
     * <p>The most efficient way to access and/or modify the samples in the raw data buffer is using
     * the following nested loops:
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
     * <p>If the absolute x,y pixel coordinates are not required, the following construct maybe more
     * readable:
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
     * Gets the underlying raw sample array of type <code>byte</code> (signed or unsigned).
     * If the underlying data buffer is not of type <code>byte</code>, <code>null</code> is returned.
     * <p>Refer to {@link #getDataBuffer()} for using the primitive array.
     *
     * @return The underlying data buffer's primitive array, or <code>null</code> (see above).
     *
     * @see #getDataBufferIndex(int, int)
     * @see #getDataBuffer()
     */
    byte[] getDataBufferByte();

    /**
     * Gets the underlying raw sample array of type <code>short</code> (signed or unsigned).
     * If the underlying data buffer is not of type <code>short</code>, <code>null</code> is returned.
     * <p>Refer to {@link #getDataBuffer()} for using the primitive array.
     *
     * @return The underlying data buffer's primitive array, or <code>null</code> (see above).
     *
     * @see #getDataBufferIndex(int, int)
     * @see #getDataBuffer()
     */
    short[] getDataBufferShort();

    /**
     * Gets the underlying raw sample array of type <code>int</code>.
     * If the underlying data buffer is not of type <code>int</code>, <code>null</code> is returned.
     * <p>Refer to {@link #getDataBuffer()} for using the primitive array.
     *
     * @return The underlying data buffer's primitive array, or <code>null</code> (see above).
     *
     * @see #getDataBufferIndex(int, int)
     * @see #getDataBuffer()
     */
    int[] getDataBufferInt();

    /**
     * Gets the underlying raw sample array of type <code>float</code>.
     * If the underlying data buffer is not of type <code>float</code>, <code>null</code> is returned.
     * <p>Refer to {@link #getDataBuffer()} for using the primitive array.
     *
     * @return The underlying data buffer's primitive array, or <code>null</code> (see above).
     *
     * @see #getDataBufferIndex(int, int)
     * @see #getDataBuffer()
     */
    float[] getDataBufferFloat();

    /**
     * Gets the underlying raw sample array of type <code>double</code>.
     * If the underlying data buffer is not of type <code>double</code>, <code>null</code> is returned.
     * <p>Refer to {@link #getDataBuffer()} for using the primitive array.
     *
     * @return The underlying data buffer's primitive array, or <code>null</code> (see above).
     *
     * @see #getDataBufferIndex(int, int)
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
     * Gets the raw (unscaled, uncalibrated) samples, e.g. detector counts, copied from or wrapping the underlying
     * data buffer. In contradiction to the {@link #getDataBuffer()} method, the returned samples
     * will cover exactly the region {@link #getRectangle()} rectangle} of this tile. Thus, the number
     * of returned samples will always equal {@link #getWidth() width} {@code *} {@link #getHeight() height}.
     * <p>In order to apply changes of the samples values to this tile, it is mandatory to call
     * {@link #setRawSamples(ProductData)} with the modified
     * {@code ProductData} instance.
     *
     * @return The raw samples copied from or wrapping the underlying data buffer.
     */
    ProductData getRawSamples();

    /**
     * Sets this tile's raw (unscaled, uncalibrated) samples.
     * The number of given samples must be equal {@link #getWidth() width} {@code *} {@link #getHeight() height}
     * of this tile.
     * <p>This method must be used
     * in order to apply changes made to the samples returned by the {@link #getRawSamples()} method.
     *
     * @param rawSamples The raw samples to be set.
     */
    void setRawSamples(ProductData rawSamples);

    /**
     * Gets the scaled, (geo-)physical array of {@code int} samples, copied from or directly returning the underlying
     * data buffer. In contradiction to the {@link #getDataBuffer()} method, the returned samples
     * will cover exactly the region {@link #getRectangle()} rectangle} of this tile. Thus, the number
     * of returned samples will always equal {@link #getWidth() width} {@code *} {@link #getHeight() height}.
     * <p>
     * Sample values that are masked out (see {@link #isSampleValid(int, int)}) are set to zero.
     *
     * @return The (geo-)physical samples computed from the underlying raw data buffer.
     *
     * @see #setSamples(byte[])
     * @since BEAM 5.0
     */
    byte[] getSamplesByte();

    /**
     * Gets the scaled, (geo-)physical array of {@code int} samples, copied from or directly returning the underlying
     * data buffer. In contradiction to the {@link #getDataBuffer()} method, the returned samples
     * will cover exactly the region {@link #getRectangle()} rectangle} of this tile. Thus, the number
     * of returned samples will always equal {@link #getWidth() width} {@code *} {@link #getHeight() height}.
     * <p>
     * Sample values that are masked out (see {@link #isSampleValid(int, int)}) are set to zero.
     *
     * @return The (geo-)physical samples computed from the underlying raw data buffer.
     *
     * @see #setSamples(short[])
     * @since BEAM 5.0
     */
    short[] getSamplesShort();

    /**
     * Gets the scaled, (geo-)physical array of {@code int} samples, copied from or directly returning the underlying
     * data buffer. In contradiction to the {@link #getDataBuffer()} method, the returned samples
     * will cover exactly the region {@link #getRectangle()} rectangle} of this tile. Thus, the number
     * of returned samples will always equal {@link #getWidth() width} {@code *} {@link #getHeight() height}.
     * <p>
     * Sample values that are masked out (see {@link #isSampleValid(int, int)}) are set to zero.
     *
     * @return The (geo-)physical samples computed from the underlying raw data buffer.
     *
     * @see #setSamples(int[])
     * @since BEAM 4.8
     */
    int[] getSamplesInt();

    /**
     * Gets the scaled, (geo-)physical array of {@code double} samples, copied from or directly returning the underlying
     * data buffer. In contradiction to the {@link #getDataBuffer()} method, the returned samples
     * will cover exactly the region {@link #getRectangle()} rectangle} of this tile. Thus, the number
     * of returned samples will always equal {@link #getWidth() width} {@code *} {@link #getHeight() height}.
     * <p>
     * Sample values that are masked out (see {@link #isSampleValid(int, int)}) are set to {@link Float#NaN}.
     *
     * @return The (geo-)physical samples computed from the underlying raw data buffer.
     *
     * @see #setSamples(float[])
     */
    float[] getSamplesFloat();

    /**
     * Gets the scaled, (geo-)physical array of {@code double} samples, copied from or directly returning the underlying
     * data buffer. In contradiction to the {@link #getDataBuffer()} method, the returned samples
     * will cover exactly the region {@link #getRectangle()} rectangle} of this tile. Thus, the number
     * of returned samples will always equal {@link #getWidth() width} {@code *} {@link #getHeight() height}.
     * <p>
     * Sample values that are masked out (see {@link #isSampleValid(int, int)}) are set to {@link Double#NaN}.
     *
     * @return The (geo-)physical samples computed from the underlying raw data buffer.
     *
     * @see #setSamples(double[])
     */
    double[] getSamplesDouble();

    /**
     * Sets this tile's scaled, (geo-)physical samples as array of {@code floats}s.
     * The number of given samples must be equal {@link #getWidth() width} {@code *} {@link #getHeight() height}
     * of this tile.
     *
     * @param samples The (geo-)physical samples to be set.
     *
     * @see #getSamplesByte()
     * @since BEAM 5.0
     */
    void setSamples(byte[] samples);

    /**
     * Sets this tile's scaled, (geo-)physical samples as array of {@code floats}s.
     * The number of given samples must be equal {@link #getWidth() width} {@code *} {@link #getHeight() height}
     * of this tile.
     *
     * @param samples The (geo-)physical samples to be set.
     *
     * @see #getSamplesShort()
     * @since BEAM 5.0
     */
    void setSamples(short[] samples);

    /**
     * Sets this tile's scaled, (geo-)physical samples as array of {@code floats}s.
     * The number of given samples must be equal {@link #getWidth() width} {@code *} {@link #getHeight() height}
     * of this tile.
     *
     * @param samples The (geo-)physical samples to be set.
     *
     * @see #getSamplesInt()
     * @since BEAM 4.8
     */
    void setSamples(int[] samples);

    /**
     * Sets this tile's scaled, (geo-)physical samples as array of {@code floats}s.
     * The number of given samples must be equal {@link #getWidth() width} {@code *} {@link #getHeight() height}
     * of this tile.
     *
     * @param samples The (geo-)physical samples to be set.
     *
     * @see #getSamplesFloat()
     */
    void setSamples(float[] samples);

    /**
     * Sets this tile's scaled, (geo-)physical samples as array of {@code double}s.
     * The number of given samples must be equal {@link #getWidth() width} {@code *} {@link #getHeight() height}
     * of this tile.
     * <p>This method must be used
     * in order to apply changes made to the samples returned by the {@link #getRawSamples()} method.
     *
     * @param samples The (geo-)physical samples to be set.
     *
     * @see #getSamplesDouble()
     */
    void setSamples(double[] samples);

    /**
     * Checks whether or not the sample value exists and is valid at a given image pixel position.
     *
     * @param x the image pixel x-coordinate
     * @param y the image pixel y-coordinate
     *
     * @return true, if the sample exists and is valid
     *
     * @since BEAM 4.7.1
     */
    boolean isSampleValid(int x, int y);

    /**
     * Gets the (geo-)physically scaled sample at the given pixel coordinate as {@code boolean} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.
     * <p>Note that in most cases, accessing the tile's {@link #getDataBuffer() dataBuffer} directly in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties
     * gives a better performance.
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     *
     * @return The (geo-)physical sample as {@code boolean} value.
     */
    boolean getSampleBoolean(int x, int y);

    /**
     * Sets the (geo-)physically scaled sample at the given pixel coordinate from a {@code boolean} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.
     * <p>Note that in most cases, accessing the tile's {@link #getDataBuffer() dataBuffer} directly in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties
     * gives a better performance.
     *
     * @param x      The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y      The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param sample The (geo-)physically scaled sample as {@code boolean} value.
     */
    void setSample(int x, int y, boolean sample);

    /**
     * Gets the (geo-)physically scaled sample at the given pixel coordinate as {@code int} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.
     * <p>Note that in most cases, accessing the tile's {@link #getDataBuffer() dataBuffer} directly in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties
     * gives a better performance.
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     *
     * @return The (geo-)physically scaled sample as {@code int} value.
     */
    int getSampleInt(int x, int y);

    /**
     * Sets the (geo-)physically scaled sample at the given pixel coordinate from a {@code int} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.
     * The conversion ensures that no overflow happens. If necessary the value is cropped to the value range.
     * <p>Note that in most cases, accessing the tile's {@link #getDataBuffer() dataBuffer} directly in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties
     * gives a better performance.
     *
     * @param x      The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y      The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param sample The (geo-)physically scaled sample as {@code int} value.
     */
    void setSample(int x, int y, int sample);

    /**
     * Gets the (geo-)physically scaled sample at the given pixel coordinate as {@code float} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     *
     * @return The (geo-)physically scaled sample as {@code float} value.
     */
    float getSampleFloat(int x, int y);

    /**
     * Sets the (geo-)physically scaled sample at the given pixel coordinate from a {@code float} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.
     * The conversion ensures that no overflow happens. If necessary the value is cropped to the value range.
     * <p>Note that in most cases, accessing the tile's {@link #getDataBuffer() dataBuffer} directly in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties
     * gives a better performance.
     *
     * @param x      The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y      The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param sample The (geo-)physically scaled sample as {@code float} value.
     */
    void setSample(int x, int y, float sample);

    /**
     * Gets the (geo-)physically scaled sample value for the given pixel coordinate as {@code double}.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.
     * <p>Note that in most cases, accessing the tile's {@link #getDataBuffer() dataBuffer} directly in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties
     * gives a better performance.
     *
     * @param x The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     *
     * @return The (geo-)physically scaled sample as {@code double} value.
     */
    double getSampleDouble(int x, int y);

    /**
     * Sets the (geo-)physically scaled sample at the given pixel coordinate from a {@code double} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.
     * The conversion ensures that no overflow happens. If necessary the value is cropped to the value range.
     * <p>Note that in most cases, accessing the tile's {@link #getDataBuffer() dataBuffer} directly in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties
     * gives a better performance.
     *
     * @param x      The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y      The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param sample The (geo-)physically scaled sample as {@code double} value.
     */
    void setSample(int x, int y, double sample);

    /**
     * Gets the bit-coded sample value for the given pixel coordinate and the specified bit index as a {@code boolean}.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.
     *
     * @param x        The absolute pixel x-coordinate, must be greater or equal {@link #getMinX()} and less or equal {@link #getMaxX()}.
     * @param y        The absolute pixel y-coordinate, must be greater or equal {@link #getMinY()} and less or equal {@link #getMaxY()}.
     * @param bitIndex The bit index.
     *
     * @return The sample as {@code boolean} value.
     */
    boolean getSampleBit(int x, int y, int bitIndex);

    /**
     * Sets the bit-coded sample at the given pixel coordinate and the specified bit index from a {@code boolean} value.
     * <p>If the underlying data buffer is of a different sample data type, an appropriate type conversion is performed.
     * <p>Note that in most cases it is more performant to directly access the tile's {@link #getDataBuffer() dataBuffer} in conjunction
     * with the {@link #getScanlineOffset() scanlineOffset} and {@link #getScanlineStride() scanlineStride} properties.
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
     * for (int y = tile.getMinY(); y &lt;= tile.getMaxY(); y++) {
     *     for (int x = tile.getMinX(); x &lt;= tile.getMaxX(); x++) {
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
