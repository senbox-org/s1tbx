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

package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.math.DoubleList;

import javax.media.jai.UnpackedImageData;
import java.awt.image.DataBuffer;

/**
 * A statistics operator.
 *
 * @author Norman Fomferra
 * @since BEAM 4.5.1, full revision in 4.10
 */
abstract class StxOp {

    private final String name;

    protected StxOp(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void accumulateData(UnpackedImageData dataPixels,
                                        UnpackedImageData maskPixels);

    static DoubleList asDoubleList(UnpackedImageData dataPixels) {
        if (dataPixels.type == DataBuffer.TYPE_BYTE) {
            return new DoubleList.UByte(dataPixels.getByteData(0));
        } else if (dataPixels.type == DataBuffer.TYPE_SHORT) {
            return new DoubleList.Short(dataPixels.getShortData(0));
        } else if (dataPixels.type == DataBuffer.TYPE_USHORT) {
            return new DoubleList.UShort(dataPixels.getShortData(0));
        } else if (dataPixels.type == DataBuffer.TYPE_INT) {
            return new DoubleList.Int(dataPixels.getIntData(0));
        } else if (dataPixels.type == DataBuffer.TYPE_FLOAT) {
            return new DoubleList.Float(dataPixels.getFloatData(0));
        } else if (dataPixels.type == DataBuffer.TYPE_DOUBLE) {
            return new DoubleList.Double(dataPixels.getDoubleData(0));
        } else {
            return new ZeroDoubleList(dataPixels.rect.width * dataPixels.rect.height);
        }
    }

    final static class ZeroDoubleList implements DoubleList {

        private final int size;

        public ZeroDoubleList(int size) {
            this.size = size;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public double getDouble(int index) {
            return 0.0;
        }
    }
}
