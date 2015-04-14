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


import junit.framework.TestCase;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TiffDirectoryEntryTest extends TestCase {

    public void testCreation_WithOneValue() {
        final TiffShort tag = new TiffShort(3);
        final int expValue = 7643;
        final TiffLong value = new TiffLong(expValue);

        final TiffDirectoryEntry entry = new TiffDirectoryEntry(tag, value);

        assertEquals(tag.getValue(), entry.getTag().getValue());
        assertEquals(TiffType.LONG.getValue(), entry.getType().getValue());
        assertEquals(1, entry.getCount().getValue());
        assertEquals(expValue, ((TiffLong) entry.getValues()[0]).getValue());
    }

    public void testCreation_WithValueArray() {
        final TiffShort tag = new TiffShort(3);
        final int[] values = new int[]{7643, 974646};
        final TiffValue[] tiffValues = createTiffLongValues(values);

        final TiffDirectoryEntry entry = new TiffDirectoryEntry(tag, tiffValues);

        assertEquals(tag.getValue(), entry.getTag().getValue());
        assertEquals(TiffType.LONG.getValue(), entry.getType().getValue());
        assertEquals(values.length, entry.getCount().getValue());
        final TiffValue[] actualValues = entry.getValues();
        for (int i = 0; i < actualValues.length; i++) {
            final TiffLong tiffLong = (TiffLong) actualValues[i];
            assertEquals("failure at index " + i, values[i], tiffLong.getValue());
        }
    }

    public void testCreation_ValueTypesAreMixed() {
        final TiffShort tag = new TiffShort(3);
        final TiffValue[] mixedValueTypes = new TiffValue[]{
                new TiffLong(23445),
                new TiffShort(123),
        };
        try {
            new TiffDirectoryEntry(tag, mixedValueTypes);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {

        } catch (Exception e) {
            fail("IllegalArgumentException expected");
        }
    }

    public void testWriteToStream() throws IOException {
        //preparation
        final TiffValue[] values = new TiffValue[]{
                new TiffShort(23),
                new TiffShort(24),
                new TiffShort(25),
                new TiffShort(26),
                new TiffShort(27),
                new TiffShort(28),
        };
        final TiffShort tag = new TiffShort(3);
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(tag, values);
        final int entryOffset = 23;
        final long valuesOffset = 100;
        final MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());

        //execution of the method under test
        stream.seek(entryOffset);
        entry.setValuesOffset(valuesOffset);
        entry.write(stream);

        //validate stream length
        assertEquals(getExpectedStreamLength(values, valuesOffset), stream.length());

        //validate written entry data
        stream.seek(entryOffset);
        final TiffShort type = TiffType.getType(values);
        assertEquals(tag.getValue(), stream.readShort());
        assertEquals(type.getValue(), stream.readShort());
        assertEquals(values.length, stream.readInt());
        assertEquals(valuesOffset, stream.readInt());

        //validate written entry values
        stream.seek(valuesOffset);
        for (int i = 0; i < values.length; i++) {
            final TiffShort value = (TiffShort) values[i];
            assertEquals("failure at index " + i, value.getValue(), stream.readShort());
        }
    }

    public void testWriteToStream_OneTiffShortValue() throws IOException {
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(232), new TiffShort(28));

        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());
        entry.write(ios);

        assertEquals(12, ios.length());
        ios.seek(0);
        assertEquals(232, ios.readShort());
        assertEquals(TiffType.SHORT_TYPE, ios.readShort());
        assertEquals(1, ios.readInt());
        assertEquals(28, ios.readShort());
        assertEquals(0, ios.readShort());
    }

    public void testWriteToStream_TwoTiffShortValue() throws IOException {
        final TiffShort[] values = new TiffShort[]{new TiffShort(28), new TiffShort(235)};
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(232), values);

        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());
        entry.write(ios);

        assertEquals(12, ios.length());
        ios.seek(0);
        assertEquals(232, ios.readShort());
        assertEquals(TiffType.SHORT_TYPE, ios.readShort());
        assertEquals(values.length, ios.readInt());
        for (int i = 0; i < values.length; i++) {
            assertEquals("at index [" + i + "] ", values[i].getValue(), ios.readShort());
        }
    }

    public void testWrite_ValuesSizeInBytesIsBiggerThanFour_ValueOffsetIsNull() {
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(3), new TiffRational(12, 6));
        final MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());

        try {
            entry.write(stream);
            fail("IllegalStateException expected because offset for values are not applied");
        } catch (IllegalStateException expected) {

        } catch (Exception notExpected) {
            fail("IllegalStateException expected, but was " + notExpected.getClass().getName());
        }
    }

    public void testGetSize_TiffShort_OneValue() {
        final TiffValue[] values = new TiffValue[]{
                new TiffShort(30),
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(12), values);

        assertEquals(TiffDirectoryEntry.BYTES_PER_ENTRY, entry.getSize());
    }

    public void testGetSize_TiffShort_TwoValues() {
        final TiffValue[] values = new TiffValue[]{
                new TiffShort(30),
                new TiffShort(20),
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(12), values);

        assertEquals(TiffDirectoryEntry.BYTES_PER_ENTRY, entry.getSize());
    }

    public void testGetSize_TiffShort_MoreThanTwoValues() {
        final TiffValue[] values = new TiffValue[]{
                new TiffShort(1),
                new TiffShort(5),
                new TiffShort(10)
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(12), values);

        final int expectedSize = TiffDirectoryEntry.BYTES_PER_ENTRY + 6;
        assertEquals(expectedSize, entry.getSize());
    }

    public void testGetSize_TiffLong_OneValue() {
        final TiffValue[] values = new TiffValue[]{
                new TiffLong(30)
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(12), values);

        assertEquals(TiffDirectoryEntry.BYTES_PER_ENTRY, entry.getSize());
    }

    public void testGetSize_TiffLong_ValueArray() {
        final TiffValue[] values = new TiffValue[]{
                new TiffLong(1),
                new TiffLong(5),
                new TiffLong(10)
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(12), values);

        final int expectedSize = TiffDirectoryEntry.BYTES_PER_ENTRY + 12;
        assertEquals(expectedSize, entry.getSize());
    }

    public void testGetSize_TiffRational_OneValue() {
        final TiffValue[] values = new TiffValue[]{
                new TiffRational(20, 30)
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(12), values);

        final int expectedSize = TiffDirectoryEntry.BYTES_PER_ENTRY + 8;
        assertEquals(expectedSize, entry.getSize());
    }

    public void testGetSize_TiffRational_ValueArray() {
        final TiffValue[] values = new TiffValue[]{
                new TiffRational(4, 1),
                new TiffRational(7, 5),
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(12), values);

        final int expectedSize = TiffDirectoryEntry.BYTES_PER_ENTRY + 16;
        assertEquals(expectedSize, entry.getSize());
    }

    public void testGetValueSizeInBytes_TiffShort() {
        final TiffValue[] values = new TiffValue[]{
                new TiffShort(30),
                new TiffShort(430),
                new TiffShort(50),
                new TiffShort(370),
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(12), values);

        assertEquals(8, entry.getValuesSizeInBytes());
    }

    public void testGetValueSizeInBytes_TiffLong() {
        final TiffValue[] values = new TiffValue[]{
                new TiffLong(30),
                new TiffLong(430),
                new TiffLong(50),
                new TiffLong(370),
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(12), values);

        assertEquals(16, entry.getValuesSizeInBytes());
    }

    public void testGetValueSizeInBytes_TiffRational() {
        final TiffValue[] values = new TiffValue[]{
                new TiffRational(30, 10),
                new TiffRational(430, 20),
                new TiffRational(50, 34),
                new TiffRational(370, 32),
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(12), values);

        assertEquals(32, entry.getValuesSizeInBytes());
    }

    public void testGetValueSizeInBytes_TiffAscii() {
        final TiffValue[] values = new TiffValue[]{
                new TiffAscii("ab"),   // 3 bytes
                new TiffAscii("kls"),  // 4 bytes
                new TiffAscii("50."),  // 4 bytes
                new TiffAscii("1"),    // 2 bytes
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(TiffTag.COMPRESSION, values);

        assertEquals(13, entry.getValuesSizeInBytes());
    }

    public void testMustValuesBeReferenced_TiffShort_OneValue() {
        final TiffValue[] values = new TiffValue[]{
                new TiffShort(12)
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(23), values);

        assertFalse(entry.mustValuesBeReferenced());
    }

    public void testMustValuesBeReferenced_TiffShort_TwoValues() {
        final TiffValue[] values = new TiffValue[]{
                new TiffShort(12),
                new TiffShort(14),
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(23), values);

        assertFalse(entry.mustValuesBeReferenced());
    }

    public void testMustValuesBeReferenced_TiffShort_MoreThanTwoValues() {
        final TiffValue[] values = new TiffValue[]{
                new TiffShort(12),
                new TiffShort(14),
                new TiffShort(16),
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(23), values);

        assertTrue(entry.mustValuesBeReferenced());
    }

    public void testMustValuesBeReferenced_TiffLong_OneValue() {
        final TiffValue[] values = new TiffValue[]{
                new TiffLong(12)
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(23), values);

        assertFalse(entry.mustValuesBeReferenced());
    }

    public void testMustValuesBeReferenced_TiffLong_MoreThanOneValue() {
        final TiffValue[] values = new TiffValue[]{
                new TiffLong(12),
                new TiffLong(14),
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(23), values);

        assertTrue(entry.mustValuesBeReferenced());
    }

    public void testMustValuesBeReferenced_TiffRational_OneValue() {
        final TiffValue[] values = new TiffValue[]{
                new TiffRational(12, 14)
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(23), values);

        assertTrue(entry.mustValuesBeReferenced());
    }

    public void testMustValuesBeReferenced_TiffRational_MoreThanOneValue() {
        final TiffValue[] values = new TiffValue[]{
                new TiffRational(12, 6),
                new TiffRational(14, 5),
        };
        final TiffDirectoryEntry entry = new TiffDirectoryEntry(new TiffShort(23), values);

        assertTrue(entry.mustValuesBeReferenced());
    }

    private long getExpectedStreamLength(final TiffValue[] values, final long valuesOffset) {
        final TiffShort type = TiffType.getType(values);
        final short bytesForType = TiffType.getBytesForType(type);
        return values.length * bytesForType + valuesOffset;
    }

    private TiffValue[] createTiffLongValues(final int[] ints) {
        final TiffValue[] values = new TiffValue[ints.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = new TiffLong(ints[i]);
        }
        return values;
    }

// TOCO - Check: Never used?    
//    private TiffValue[] createTiffShortValues(final int[] ints) {
//        final TiffValue[] values = new TiffValue[ints.length];
//        for (int i = 0; i < values.length; i++) {
//            values[i] = new TiffShort(ints[i]);
//        }
//        return values;
//    }

}
