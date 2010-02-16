/*
 * $Id: ModisUint16PowBandReader.java,v 1.3 2007/03/19 15:52:28 marcop Exp $
 *
 * Copyright (C) 2002,2003  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.dataio.obpg.bandreader;

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.obpg.hdf.lib.HDF;
import org.esa.beam.framework.datamodel.ProductData;

public class ObpgUint16PowBandReader extends ObpgBandReader {

    private short[] _line;
    private int min;
    private int max;
    private short fill;
    private float[] targetData;
    private int targetIdx;

    public ObpgUint16PowBandReader(final int sdsId, final int layer, final boolean is3d) {
        super(sdsId, layer, is3d);
    }

    /**
     * Retrieves the data type of the band
     *
     * @return always {@link org.esa.beam.framework.datamodel.ProductData#TYPE_FLOAT32}
     */
    @Override
    public int getDataType() {
        return ProductData.TYPE_FLOAT32;
    }

    @Override
    protected void prepareForReading(final int sourceOffsetX, final int sourceOffsetY, final int sourceWidth,
                                     final int sourceHeight,  final int sourceStepX,   final int sourceStepY,
                                     final ProductData destBuffer) {
        fill = (short) Math.floor(_fillValue + 0.5);
        if (_validRange == null) {
            min = 0;
            max = Short.MAX_VALUE * 2 + 1;
        } else {
            min = (int) Math.floor(_validRange.getMin() + 0.5);
            max = (int) Math.floor(_validRange.getMax() + 0.5);
        }
        targetData = (float[]) destBuffer.getElems();
        targetIdx = 0;
        ensureLineWidth(sourceWidth);
    }

    @Override
    protected void readLine() throws HDFException {
        HDF.getInstance().SDreaddata(_sdsId, _start, _stride, _count, _line);
    }

    @Override
    protected void validate(final int x) {
        final int value = _line[x] & 0xffff;
        if (value < min || value > max) {
            _line[x] = fill;
        }
    }

    @Override
    protected void assign(final int x) {
        targetData[targetIdx++] = (float) Math.pow(10.f, (_scale * _line[x] + _offset));
    }

    private void ensureLineWidth(final int sourceWidth) {
        if ((_line == null) || (_line.length != sourceWidth)) {
            _line = new short[sourceWidth];
        }
    }
}