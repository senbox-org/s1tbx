/*
 * $Id: ModisUint16BandReader.java,v 1.4 2007/04/17 10:03:50 marcop Exp $
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
import org.esa.beam.dataio.modis.hdf.lib.HDF;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.ProductData;

public class ModisUint16BandReader extends ModisBandReader {

    private short[] _line;

    public ModisUint16BandReader(final int sdsId, final int layer, final boolean is3d) {
        super(sdsId, layer, is3d);
    }

    /**
     * Retrieves the data type of the band
     *
     * @return always {@link ProductData#TYPE_UINT16}
     */
    @Override
    public int getDataType() {
        return ProductData.TYPE_UINT16;
    }

    @Override
    public void readBandDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                 int sourceStepX,
                                 int sourceStepY, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                 ProductData destBuffer, ProgressMonitor pm) throws HDFException, ProductIOException {
        final int min;
        final int max;
        final short fill = (short) Math.floor(_fillValue + 0.5);
        if (_validRange == null) {
            min = 0;
            max = Short.MAX_VALUE * 2 + 1;
        } else {
            min = (int) Math.floor(_validRange.getMin() + 0.5);
            max = (int) Math.floor(_validRange.getMax() + 0.5);
        }

        final short[] targetData = (short[]) destBuffer.getElems();
        int targetIdx = 0;

        ensureLineWidth(sourceWidth);

        pm.beginTask("Reading band '" + getName() + "'...", sourceHeight);
        // loop over lines
        try {
            for (int y = 0; y < sourceHeight; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }
                HDF.getWrap().SDreaddata(_sdsId, _start, _stride, _count, _line);
                for (int x = 0; x < sourceWidth; x++) {
                    final int value = _line[x] & 0xffff;
                    if (value < min || value > max) {
                        _line[x] = fill;
                    }
                    targetData[targetIdx] = _line[x];
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
            _line = new short[sourceWidth];
        }
    }
}
