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
import org.esa.snap.core.datamodel.TiePointGrid;

import java.io.IOException;

/**
 * Classes implementing the {@code ProductReader} interface know how to create an in-memory representation of a
 * given data product as input source.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see ProductWriter
 */
public interface ProductReader {

    /**
     * Returns the plug-in which created this product reader.
     *
     * @return the product reader plug-in, should never be {@code null}
     */
    ProductReaderPlugIn getReaderPlugIn();

    /**
     * Retrieves the current input destination object. The return value might be {@code null} if the
     * input has not been set so far.
     */
    Object getInput();

    /**
     * Returns the subset information with which this a data product is read from its physical source.
     *
     * @return the subset information, can be {@code null}
     */
    ProductSubsetDef getSubsetDef();

    /**
     * Reads a data product and returns a in-memory representation of it.
     * <p> The given subset info can be used to specify spatial and spectral portions of the original product. If the
     * subset is omitted, the complete product is read in.
     * <p> Whether the band data - the actual pixel values - is read in immediately or later when pixels are requested,
     * is up to the implementation.
     *
     * @param input     an object representing a valid output for this product reader, might be a
     *                  {@code ImageInputStream} or other {@code Object} to use for future decoding.
     * @param subsetDef a spectral or spatial subset (or both) of the product. If {@code null}, the entire product
     *                  is read in
     * @throws IllegalArgumentException   if {@code input} is {@code null} or it's type is not one of the
     *                                    supported input sources.
     * @throws IOException                if an I/O error occurs
     * @throws IllegalFileFormatException if the file format is illegal
     */
    Product readProductNodes(Object input, ProductSubsetDef subsetDef) throws IOException, IllegalFileFormatException;

    default void readTiePointGridRasterData(TiePointGrid tpg,
                                             int destOffsetX, int destOffsetY,
                                             int destWidth, int destHeight,
                                             ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IOException("readTiePointGridRasterData not implemented");
    }

    /**
     * Reads raster data from the data source specified by the given destination band into the given in-memory buffer
     * and region.
     * <h3>Destination band</h3> The destination band is used to identify the data source from which this method
     * transfers the sample values into the given destination buffer. The method does not modify the given destination
     * band at all. If this product reader has a {@code ProductSubsetDef} instance attached to it, the method
     * should also consider the specified spatial subset and sub-sampling (if any) applied to the destination band.
     * <h3>Destination region</h3> The given destination region specified by the {@code destOffsetX},
     * {@code destOffsetY}, {@code destWidth} and {@code destHeight} parameters are given in the band's
     * raster co-ordinates of the raster which results <i>after</i> applying the optional spatial subset and
     * sub-sampling given by the {@code ProductSubsetDef} instance to the <i>data source</i>. If no spatial subset
     * and sub-sampling is specified, the destination co-ordinates are identical with the source co-ordinates. The
     * destination region should always specify a sub-region of the band's scene raster.
     * <h3>Destination buffer</h3> The first element of the destination buffer corresponds to the given
     * {@code destOffsetX} and {@code destOffsetY} of the destination region. The offset parameters are
     * <b>not</b> an offset within the buffer.<br> The number of elements in the buffer exactly be {@code destWidth *
     * destHeight}. The pixel values read are stored in line-by-line order, so the raster X co-ordinate varies
     * faster than the Y co-ordinate.
     *
     * @param destBand    the destination band which identifies the data source from which to read the sample values
     * @param destOffsetX the X-offset in the band's raster co-ordinates
     * @param destOffsetY the Y-offset in the band's raster co-ordinates
     * @param destWidth   the width of region to be read given in the band's raster co-ordinates
     * @param destHeight  the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer  the destination buffer which receives the sample values to be read
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if the number of elements destination buffer not equals {@code destWidth *
     *                                  destHeight} or the destination region is out of the band's scene raster
     * @see Band#getRasterWidth()
     * @see Band#getRasterHeight()
     */
    void readBandRasterData(Band destBand,
                            int destOffsetX, int destOffsetY,
                            int destWidth, int destHeight,
                            ProductData destBuffer, ProgressMonitor pm) throws IOException;

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to {@code close()} are undefined.
     * <p>Overrides of this method should always call {@code super.close();} after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    void close() throws IOException;
}
