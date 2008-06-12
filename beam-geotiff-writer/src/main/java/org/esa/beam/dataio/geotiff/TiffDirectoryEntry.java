/*
 * Created on: 	04.02.2005
 * Created by:	MyHTPC
 * File: 		TIFFDirectoyEntry.java
 */
package org.esa.beam.dataio.geotiff;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * A directory entry implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class TiffDirectoryEntry {

    public static final short BYTES_PER_ENTRY = 12;
    private TiffShort _tag;
    private TiffShort _type;
    private TiffLong _count;
    private TiffValue[] _values;
    private TiffLong _valuesOffset;

    public TiffDirectoryEntry(final TiffShort tiffTag, final TiffValue value) {
        this(tiffTag, new TiffValue[]{value});
    }

    public TiffDirectoryEntry(final TiffShort tiffTag, final TiffValue[] values) {
        _type = TiffType.getType(values);
        _tag = tiffTag;
        _count = getCount(values);
        _values = values;
    }

    public TiffShort getTag() {
        return _tag;
    }

    public TiffLong getCount() {
        return _count;
    }

    public TiffShort getType() {
        return _type;
    }

    public TiffValue[] getValues() {
        return _values;
    }

    public void write(final ImageOutputStream ios) throws IOException {
        if (mustValuesBeReferenced() && _valuesOffset == null) {
            throw new IllegalStateException("no value offset given");
        }

        _tag.write(ios);
        _type.write(ios);
        _count.write(ios);

        if (_valuesOffset == null) {
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
        _valuesOffset = new TiffLong(offset);
    }

    public long getSize() {
        return BYTES_PER_ENTRY + getReferencedValuesSizeInBytes();
    }

    public boolean mustValuesBeReferenced() {
        return getValuesSizeInBytes() > 4;
    }

    public long getValuesSizeInBytes() {
        int size = 0;
        for (int i = 0; i < _values.length; i++) {
            size += _values[i].getSizeInBytes();
        }
        return size;
    }

    private void writeValuesReferenced(final ImageOutputStream ios) throws IOException {
        _valuesOffset.write(ios);
        ios.seek(_valuesOffset.getValue());
        writeValues(ios);
    }

    private void writeValues(final ImageOutputStream ios) throws IOException {
        for (int i = 0; i < _values.length; i++) {
            _values[i].write(ios);
        }
    }

    public TiffLong getValuesOffset() {
        return _valuesOffset;
    }

    private TiffLong getCount(final TiffValue[] values) {
        if (_type.getValue() != TiffType.ASCII.getValue()) {
            return new TiffLong(values.length);
        }
        long size = 0;
        for (int i = 0; i < values.length; i++) {
            size += values[i].getSizeInBytes();
        }
//        if (size % 2 > 0) {
//            size++;
//        }
        return new TiffLong(size);
    }
}