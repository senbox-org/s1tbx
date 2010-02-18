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
package org.esa.beam.dataio.obpg.bandreader;

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.obpg.hdf.lib.HDF;
import org.esa.beam.framework.datamodel.ProductData;


public class ObpgInt8BandReader extends ObpgBandReader {

    private byte[] _line;
    private byte min;
    private byte max;
    private byte fill;
    private byte[] targetData;
    private int targetIdx;

    public ObpgInt8BandReader(final int sdsId, final int layer, final boolean is3d) {
        super(sdsId, layer, is3d);
    }

    /**
     * Retrieves the data type of the band
     *
     * @return always {@link org.esa.beam.framework.datamodel.ProductData#TYPE_INT8}
     */
    @Override
    public int getDataType() {
        return ProductData.TYPE_INT8;
    }

    @Override
    protected void prepareForReading(final int sourceOffsetX, final int sourceOffsetY, final int sourceWidth,
                                     final int sourceHeight, final int sourceStepX, final int sourceStepY,
                                     final ProductData destBuffer) {
        fill = (byte) Math.floor(_fillValue + 0.5);
        if (_validRange == null) {
            min = Byte.MIN_VALUE;
            max = Byte.MAX_VALUE;
        } else {
            min = (byte) Math.floor(_validRange.getMin() + 0.5);
            max = (byte) Math.floor(_validRange.getMax() + 0.5);
        }
        targetData = (byte[]) destBuffer.getElems();
        targetIdx = 0;
        ensureLineWidth(sourceWidth);
    }

    @Override
    protected void readLine() throws HDFException {
        HDF.getInstance().SDreaddata(_sdsId, _start, _stride, _count, _line);
    }

    @Override
    protected void validate(final int x) {
        final byte value = _line[x];
        if (value < min || value > max) {
            _line[x] = fill;
        }
    }

    @Override
    protected void assign(final int x) {
        targetData[targetIdx++] = _line[x];
    }

    private void ensureLineWidth(final int sourceWidth) {
        if ((_line == null) || (_line.length != sourceWidth)) {
            _line = new byte[sourceWidth];
        }
    }
}