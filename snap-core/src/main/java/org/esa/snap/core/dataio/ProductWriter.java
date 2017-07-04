/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;

import java.io.IOException;

/**
 * An interface that supports writing a complete data product tree and single band rasters.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see ProductReader
 */
public interface ProductWriter {

    /**
     * Returns the plug-in which created this product writer.
     *
     * @return the product writer plug-in, should never be <code>null</code>
     */
    ProductWriterPlugIn getWriterPlugIn();

    /**
     * Retrives the current output destination object. Thie return value might be <code>null</code> if the
     * <code>setOutput</code> has not been called so far.
     *
     * @return the output
     */
    Object getOutput();

    /**
     * Writes the in-memory representation of a data product.
     * <p> Whether the band data - the actual pixel values - is written out immediately or later when pixels are
     * flushed, is up to the implementation.
     *
     * @param product the in-memory representation of the data product
     * @param output  an object representing a valid output for this writer, might be a <code>ImageOutputStream</code> or
     *                other <code>Object</code> to use for future decoding.
     *
     * @throws IllegalArgumentException if <code>output</code> is <code>null</code> or it's type is not one of the
     *                                  supported output sources.
     * @throws IOException              if an I/O error occurs
     */
    void writeProductNodes(Product product, Object output) throws IOException;

    /**
     * Writes raster data from the given in-memory source buffer into the data sink specified by the given source band
     * and region.
     * <h3>Source band</h3> The source band is used to identify the data sink in which this method transfers the sample
     * values given in the source buffer. The method does not modify the pixel data of the given source band at all.
     * <h3>Source buffer</h3> The first element of the source buffer corresponds to the given <code>sourceOffsetX</code>
     * and <code>sourceOffsetY</code> of the source region. These parameters are an offset within the band's raster data
     * and <b>not</b> an offset within the source buffer.<br> The number of elements in the buffer must be exactly be
     * <code>sourceWidth * sourceHeight</code>. The pixel values to be writte are considered to be stored in
     * line-by-line order, so the raster X co-ordinate varies faster than the Y.
     * <h3>Source region</h3> The given destination region specified by the <code>sourceOffsetX</code>,
     * <code>sourceOffsetY</code>, <code>sourceWidth</code> and <code>sourceHeight</code> parameters is given in the
     * source band's raster co-ordinates. These co-ordinates are identical with the destination raster co-ordinates
     * since product writers do not support spectral or spatial subsets.
     *
     * @param sourceBand    the source band which identifies the data sink to which to write the sample values
     * @param sourceOffsetX the X-offset in the band's raster co-ordinates
     * @param sourceOffsetY the Y-offset in the band's raster co-ordinates
     * @param sourceWidth   the width of region to be written given in the band's raster co-ordinates
     * @param sourceHeight  the height of region to be written given in the band's raster co-ordinates
     * @param sourceBuffer  the source buffer which provides the sample values to be written
     * @param pm            a monitor to inform the user about progress
     *
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if the number of elements source buffer not equals <code>sourceWidth *
     *                                  sourceHeight</code> or the source region is out of the band's raster
     * @see Band#getRasterWidth()
     * @see Band#getRasterHeight()
     */
    void writeBandRasterData(Band sourceBand,
                             int sourceOffsetX, int sourceOffsetY,
                             int sourceWidth, int sourceHeight,
                             ProductData sourceBuffer,
                             ProgressMonitor pm) throws IOException;

    /**
     * Writes all data in memory to the data sink(s) associated with this writer.
     *
     * @throws IOException if an I/O error occurs
     */
    void flush() throws IOException;

    /**
     * Closes all output streams currently open. A concrete implementation should call <code>flush</code> before
     * performing the actual close-operation.
     *
     * @throws IOException if an I/O error occurs
     */
    void close() throws IOException;

    /**
     * Returns whether the given product node is to be written.
     *
     * @param node the product node
     *
     * @return <code>true</code> if so
     */
    boolean shouldWrite(ProductNode node);

    /**
     * Returns whether this product writer writes only modified product nodes.
     *
     * @return <code>true</code> if so
     */
    boolean isIncrementalMode();

    /**
     * Enables resp. disables incremental writing of this product writer. By default, a reader should enable progress
     * listening.
     *
     * @param enabled enables or disables progress listening.
     */
    void setIncrementalMode(boolean enabled);

    /**
     * Complete deletes the physical representation of the given product from the file system.
     *
     * @throws IOException if an I/O error occurs
     */
    void deleteOutput() throws IOException;

    /**
     * Physically deletes a <code>Band</code> in a product writer's output.
     *
     * @param band The band to delete.
     */
    void removeBand(Band band);

    /**
     * Sets selectable product format for writers which handle multiple formats.
     *
     * @param formatName The name of the file format.
     */
    void setFormatName(final String formatName);
}
