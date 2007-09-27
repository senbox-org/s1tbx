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
     * Checks if this is a target tile. Non-target tiles are read only.
     *
     * @return <code>true</code> if this is a target tile.
     */
    boolean isTarget();

    /**
     * The tile rectangle in the product's scene pixel coordinates.
     * Returns <code>new Rectangle(
     * {@link #getMinX() offsetX},
     * {@link #getMinY() offsetY},
     * {@link #getWidth() width},
     * {@link #getHeight() height})</code>.
     *
     * @return the tile rectangle
     */
    Rectangle getRectangle();

    /**
     * Gets the x-offset of the {@link #getRectangle() rectangle} within the scene covered by the {@link RasterDataNode}.
     *
     * @return the x-offset
     */
    int getMinX();

    /**
     * Gets the y-offset of the {@link #getRectangle() rectangle} within the scene covered by the {@link RasterDataNode}.
     *
     * @return the y-offset
     */
    int getMinY();

    /**
     * Gets the width of the {@link #getRectangle() rectangle} within the scene covered by the {@link RasterDataNode}.
     *
     * @return the width
     */
    int getWidth();

    /**
     * Gets the height of the {@link #getRectangle() rectangle} within the scene covered by the {@link RasterDataNode}.
     *
     * @return the height
     */
    int getHeight();

    /**
     * The raster data node to which this raster belongs to.
     *
     * @return the raster data node of a data product, e.g. a {@link org.esa.beam.framework.datamodel.Band} or
     *         {@link org.esa.beam.framework.datamodel.TiePointGrid TiePointGrid}.
     */
    RasterDataNode getRasterDataNode();

    /**
     * Gets the raw (unscaled, uncalibrated) sample data (e.g. detector counts) of this tile's underlying raster.
     * <p>The number of samples equals
     * <code>width*height</code> of this tile's {@link #getRectangle() rectangle}.</p>
     * <p>Note: Changing the samples will not necessarily
     * alter the underlying tile raster data before the {@link #setRawSampleData(org.esa.beam.framework.datamodel.ProductData) setProductData()}
     * method is called with the modified sample data.</p>
     *
     * @return the sample data
     */
    ProductData getRawSampleData();

    /**
     * Sets the raw (unscaled, uncalibrated) sample data (e.g. detector counts) of this tile's underlying raster.
     * <p>The number of samples must equal
     * <code>width*height</code> of this tile's {@link #getRectangle() rectangle}.</p>
     *
     * @param sampleData the sample data
     * @see #getRawSampleData()
     */
    void setRawSampleData(ProductData sampleData);

    // todo (nf, 27.09.2007) - API revision:
    // byte[] getRawSamplesByte();
    // short[] getRawSamplesShort();
    // int[] getRawSamplesInt();
    // float[] getRawSamplesFloat();
    // double[] getRawSamplesDouble();
    // int getScanlineOffset();
    // int getScanlineStride();

    int getSampleInt(int x, int y);

    void setSample(int x, int y, int v);

    float getSampleFloat(int x, int y);

    void setSample(int x, int y, float v);

    double getSampleDouble(int x, int y);

    void setSample(int x, int y, double v);

    boolean getSampleBoolean(int x, int y);

    void setSample(int x, int y, boolean v);
}
