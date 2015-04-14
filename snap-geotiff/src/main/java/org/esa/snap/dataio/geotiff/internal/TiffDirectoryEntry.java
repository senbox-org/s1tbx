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

package org.esa.snap.dataio.geotiff.internal;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * A directory entry implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
public class TiffDirectoryEntry {

    public static final short BYTES_PER_ENTRY = 12;
    private TiffShort tag;
    private TiffShort type;
    private TiffLong count;
    private TiffValue[] values;
    private TiffLong valuesOffset;

    public TiffDirectoryEntry(final TiffShort tiffTag, final TiffValue value) {
        this(tiffTag, new TiffValue[]{value});
    }

    public TiffDirectoryEntry(final TiffShort tiffTag, final TiffValue[] values) {
        type = TiffType.getType(values);
        tag = tiffTag;
        count = getCount(values);
        this.values = values;
    }

    public TiffShort getTag() {
        return tag;
    }

    public TiffLong getCount() {
        return count;
    }

    public TiffShort getType() {
        return type;
    }

    public TiffValue[] getValues() {
        return values;
    }

    public void write(final ImageOutputStream ios) throws IOException {
        if (mustValuesBeReferenced() && valuesOffset == null) {
            throw new IllegalStateException("no value offset given");
        }

        tag.write(ios);
        type.write(ios);
        count.write(ios);

        if (valuesOffset == null) {
            writeValuesInsideEnty(ios);
        } else {
            writeValuesReferenced(ios);
        }
    }

    private void writeValuesInsideEnty(final ImageOutputStream ios) throws IOException {
        writeValues(ios);
        fillEntry(ios);
    }

    private void fillEntry(final ImageOutputStream ios) throws IOException {
        final long bytesToWrite = 4 - getValuesSizeInBytes();
        for (int i = 0; i < bytesToWrite; i++) {
            ios.writeByte(0);
        }
    }

    private long getReferencedValuesSizeInBytes() {
        if (mustValuesBeReferenced()) {
            return getValuesSizeInBytes();
        } else {
            return 0;
        }
    }

    public void setValuesOffset(final long offset) {
        valuesOffset = new TiffLong(offset);
    }

    public long getSize() {
        return BYTES_PER_ENTRY + getReferencedValuesSizeInBytes();
    }

    public boolean mustValuesBeReferenced() {
        return getValuesSizeInBytes() > 4;
    }

    public long getValuesSizeInBytes() {
        int size = 0;
        for (TiffValue _value : values) {
            size += _value.getSizeInBytes();
        }
        return size;
    }

    private void writeValuesReferenced(final ImageOutputStream ios) throws IOException {
        valuesOffset.write(ios);
        ios.seek(valuesOffset.getValue());
        writeValues(ios);
    }

    private void writeValues(final ImageOutputStream ios) throws IOException {
        for (int i = 0; i < values.length; i++) {
            values[i].write(ios);
        }
    }

    public TiffLong getValuesOffset() {
        return valuesOffset;
    }

    private TiffLong getCount(final TiffValue[] values) {
        if (type.getValue() != TiffType.ASCII.getValue()) {
            return new TiffLong(values.length);
        }
        long size = 0;
        for (TiffValue value : values) {
            size += value.getSizeInBytes();
        }
        return new TiffLong(size);
    }
}
