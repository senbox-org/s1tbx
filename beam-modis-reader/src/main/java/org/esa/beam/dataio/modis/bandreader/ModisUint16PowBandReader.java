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

import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;

import java.io.IOException;

public class ModisUint16PowBandReader extends ModisBandReader {

    private short[] _line;
    private int min;
    private int max;
    private short fill;
    private float[] targetData;
    private int targetIdx;

    public ModisUint16PowBandReader(Variable variable, final int layer, final boolean is3d) {
        super(variable, layer, is3d);
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
    protected void readLine() throws InvalidRangeException, IOException {
        final Section section = new Section(_start, _count, _stride);
        final Array array = variable.read(section);
        final Object storage = array.getStorage();
        System.arraycopy(storage, 0, _line, 0, _count[0]);
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
