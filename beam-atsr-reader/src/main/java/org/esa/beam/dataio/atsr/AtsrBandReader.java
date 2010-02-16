/*
 * $Id: AtsrBandReader.java,v 1.3 2007/03/19 15:52:28 marcop Exp $
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
package org.esa.beam.dataio.atsr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

abstract class AtsrBandReader {

    protected String _bandName;
    protected long _startOffset;
    protected long _currentOffset;
    protected float _multiplier;
    protected ImageInputStream _stream;

    /**
     * Creates the object with given band name, file offset conversion multiplier and file stream.
     */
    AtsrBandReader(String bandName, int offset, float mult, ImageInputStream stream) {
        _bandName = bandName;
        _startOffset = offset;
        _multiplier = mult;
        _stream = stream;
    }

    /**
     * Retrieves the name of the band this reader belongs to.
     */
    String getBandName() {
        return _bandName;
    }

    /**
     * @deprecated in 4.0, use {@link #readBandData(int, int, int, int, int, int, int, int, int, int, org.esa.beam.framework.datamodel.ProductData, com.bc.ceres.core.ProgressMonitor)} 
     */
    void readBandData(int sourceOffsetX, int sourceOffsetY,
                               int sourceWidth, int sourceHeight,
                               int sourceStepX, int sourceStepY,
                               int destOffsetX, int destOffsetY,
                               int destWidth, int destHeight,
                               ProductData destBuffer) throws IOException {
        readBandData(sourceOffsetX, sourceOffsetY,
                     sourceWidth, sourceHeight,
                     sourceStepX, sourceStepY,
                     destOffsetX, destOffsetY,
                     destWidth, destHeight,
                     destBuffer,
                     ProgressMonitor.NULL);
    }

    /**
     * Reads the band data from file.
     */
    abstract void readBandData(int sourceOffsetX, int sourceOffsetY,
                               int sourceWidth, int sourceHeight,
                               int sourceStepX, int sourceStepY,
                               int destOffsetX, int destOffsetY,
                               int destWidth, int destHeight,
                               ProductData destBuffer,
                               ProgressMonitor pm) throws IOException;

    /**
     * Sets the file stream to the start position of the reading process
     */
    void setStreamPos(int sourceOffsetX, int sourceOffsetY, int pixelSize) throws IOException {
        // calculate offset in stream and wind to position
        _currentOffset = (sourceOffsetY * AtsrConstants.ATSR_SCENE_RASTER_WIDTH + sourceOffsetX) * pixelSize;
        _currentOffset += _startOffset;
        _stream.seek(_currentOffset);
    }

    /**
     * Updates the stream position to the position: <code>_currentOffset += delta * pixelSize</code>
     */
    void updateStreamPos(int delta, int pixelSize) throws IOException {
        _currentOffset = (int) _stream.getStreamPosition();
        _currentOffset += delta * pixelSize;
        _stream.seek(_currentOffset);
    }
}
