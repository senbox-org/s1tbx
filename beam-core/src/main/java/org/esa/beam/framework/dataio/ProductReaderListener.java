/*
 * $Id: ProductReaderListener.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.dataio;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

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
