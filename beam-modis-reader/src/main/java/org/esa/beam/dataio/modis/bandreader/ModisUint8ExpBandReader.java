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
package org.esa.beam.dataio.modis.bandreader;

import org.esa.beam.dataio.modis.hdf.lib.HDF;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

public class ModisUint8ExpBandReader extends ModisBandReader {

    private byte[] _line;
    private short min;
    private short max;
    private byte fill;
    private double invScale;
    private float[] targetData;
    private int targetDataIdx;

    public ModisUint8ExpBandReader(final int sdsId, final int layer, final boolean is3d) {
        super(sdsId, layer, is3d);
    }

    /**
     * Retrieves the data type of the band
     *
     * @return always {@link ProductData#TYPE_FLOAT32}
     */
    @Override
    public int getDataType() {
        return ProductData.TYPE_FLOAT32;
    }

    @Override
    protected void prepareForReading(final int sourceOffsetX, final int sourceOffsetY, final int sourceWidth,
                                     final int sourceHeight, final int sourceStepX, final int sourceStepY,
                                     final ProductData destBuffer) {
        fill = (byte) Math.floor(_fillValue + 0.5);
        if (_validRange == null) {
            min = 0;
            max = Byte.MAX_VALUE * 2 + 1;
        } else {
            min = (short) Math.floor(_validRange.getMin() + 0.5);
            max = (short) Math.floor(_validRange.getMax() + 0.5);
        }
        targetData = (float[]) destBuffer.getElems();
        targetDataIdx = 0;
        invScale = 1.0 / _scale;
        ensureLineWidth(sourceWidth);
    }

    @Override
    protected void readLine() throws IOException {
        HDF.getWrap().SDreaddata(_sdsId, _start, _stride, _count, _line);
    }

    @Override
    protected void validate(final int x) {
        final int value = _line[x] & 0xff;
        if (value < min || value > max) {
            _line[x] = fill;
        }
    }

    @Override
    protected void assign(final int x) {
        targetData[targetDataIdx++] = _offset * (float) Math.exp(_line[x] * invScale);
    }

    private void ensureLineWidth(final int sourceWidth) {
        if ((_line == null) || (_line.length != sourceWidth)) {
            _line = new byte[sourceWidth];
        }
    }
}
