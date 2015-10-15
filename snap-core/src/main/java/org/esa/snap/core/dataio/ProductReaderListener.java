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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;

/**
 * This imnterface must be implemented by classes that want to trace the progress of a product read operation
 */
public interface ProductReaderListener {

    /**
     * Called when a complete line has successfully been read.
     *
     * @param destBand      the destination band which identifies the data source from which to read the sample values
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the <i>current line</i> (Y-offset) in the band's raster co-ordinates
     * @param destWidth     the width of region to be read given in the band's raster co-ordinates
     * @param destHeight    the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param destBufferPos the offset within the buffer where the line which just has been read in is stored
     */
    boolean handleBandRasterLineRead(Band destBand,
                                     int destOffsetX, int destOffsetY,
                                     int destWidth, int destHeight,
                                     ProductData destBuffer, int destBufferPos);

    /**
     * @param destBand    the destination band which identifies the data source from which to read the sample values
     * @param destOffsetX the X-offset in the band's raster co-ordinates
     * @param destOffsetY the <i>current line</i> (Y-offset) in the band's raster co-ordinates
     * @param destWidth   the width of region to be read given in the band's raster co-ordinates
     * @param destHeight  the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer  the destination buffer which receives the sample values to be read
     */
    boolean handleBandRasterRectRead(Band destBand,
                                     int destOffsetX, int destOffsetY,
                                     int destWidth, int destHeight,
                                     ProductData destBuffer);
}
