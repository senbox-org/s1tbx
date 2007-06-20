/*
 * $Id: ModisInt8BandReader.java,v 1.3 2007/03/19 15:52:28 marcop Exp $
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
package org.esa.beam.dataio.modis.bandreader;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.ProductData;


public class ModisInt8BandReader extends ModisBandReader {

    private byte[] _line;

    public ModisInt8BandReader(final int sdsId, final int layer, final boolean is3d) {
        super(sdsId, layer, is3d);
    }

    /**
     * Retrieves the data type of the band
     *
     * @return always {@link ProductData#TYPE_INT8}
     */
    @Override
    public int getDataType() {
        return ProductData.TYPE_INT8;
    }

    /**
     * <p>The destination band, buffer and region parameters are exactly the ones passed to the original  call. Since
     * the <code>destOffsetX</code> and <code>destOffsetY</code> parameters are already taken into acount in the
     * <code>sourceOffsetX</code> and <code>sourceOffsetY</code> parameters, an implementor of this method is free to
     * ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be read given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be read given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be read
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the Y-offset in the band's raster co-ordinates
     * @param destWidth     the width of region to be read given in the band's raster co-ordinates
     * @param destHeight    the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param pm            a monitor to inform the user about progress
     */
    @Override
    public void readBandData(final int sourceOffsetX, final int sourceOffsetY, final int sourceWidth,
                             final int sourceHeight, final int sourceStepX, final int sourceStepY,
                             final int destOffsetX, final int destOffsetY, final int destWidth,
                             final int destHeight, final ProductData destBuffer, ProgressMonitor pm) throws
                                                                                                     HDFException,
                                                                                                     ProductIOException {
        _start[_yCoord] = sourceOffsetY;
        _start[_xCoord] = sourceOffsetX;
        _count[_yCoord] = 1;
        _count[_xCoord] = sourceWidth;
        _stride[_yCoord] = sourceStepY;
        _stride[_xCoord] = sourceStepX;

        final byte min;
        final byte max;
        final byte fill = (byte) _fillValue;
        if (_validRange != null) {
            min = (byte) _validRange.getMin();
            max = (byte) _validRange.getMax();
        } else {
            min = Byte.MIN_VALUE;
            max = Byte.MAX_VALUE;
        }

        final byte[] targetData = (byte[]) destBuffer.getElems();
        int targetIdx = 0;

        ensureLineWidth(sourceWidth);

        pm.beginTask("Reading band '" + getName() + "'...", sourceHeight);
        // loop over lines
        try {
            for (int y = 0; y < sourceHeight; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }
                HDFLibrary.SDreaddata(_sdsId, _start, _stride, _count, _line);
                for (int x = 0; x < sourceWidth; x++) {
                    if (_line[x] < min || _line[x] > max) {
                        targetData[targetIdx] = fill;
                    } else {
                        targetData[targetIdx] = _line[x];
                    }
                    ++targetIdx;
                }
                _start[_yCoord] += sourceStepY;
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    /**
     * Makes sure that the scan line buffer is present and of the correct size
     *
     * @param sourceWidth
     */
    private void ensureLineWidth(final int sourceWidth) {
        if ((_line == null) || (_line.length != sourceWidth)) {
            _line = new byte[sourceWidth];
        }
    }
}
