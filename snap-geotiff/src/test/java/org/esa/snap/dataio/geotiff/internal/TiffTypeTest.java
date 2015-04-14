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

public class TiffTypeTest extends TestCase {

    public void testGetBytesForType() {
        assertEquals(1, TiffType.getBytesForType(TiffType.BYTE));
        assertEquals(1, TiffType.getBytesForType(TiffType.ASCII));
        assertEquals(1, TiffType.getBytesForType(TiffType.SBYTE));
        assertEquals(1, TiffType.getBytesForType(TiffType.UNDEFINED));

        assertEquals(2, TiffType.getBytesForType(TiffType.SHORT));
        assertEquals(2, TiffType.getBytesForType(TiffType.SSHORT));

        assertEquals(4, TiffType.getBytesForType(TiffType.LONG));
        assertEquals(4, TiffType.getBytesForType(TiffType.SLONG));
        assertEquals(4, TiffType.getBytesForType(TiffType.FLOAT));

        assertEquals(8, TiffType.getBytesForType(TiffType.DOUBLE));
        assertEquals(8, TiffType.getBytesForType(TiffType.RATIONAL));
        assertEquals(8, TiffType.getBytesForType(TiffType.SRATIONAL));
    }

    public void testGetBytesForType_WithIllegalArgument() {
        try {
            TiffType.getBytesForType(new TiffShort(3849));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {

        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected, but was  " + notExpected.getClass().getName());
        }
    }

    public void testGetType() {
        TiffValue values[];

        values = new TiffValue[]{new TiffRational(1, 2), new TiffRational(12, 1)};
        assertEquals(TiffType.RATIONAL.getValue(), TiffType.getType(values).getValue());

        values = new TiffValue[]{new TiffLong(35486), new TiffLong(0)};
        assertEquals(TiffType.LONG.getValue(), TiffType.getType(values).getValue());

        values = new TiffValue[]{new TiffShort(5486), new TiffShort(86)};
        assertEquals(TiffType.SHORT.getValue(), TiffType.getType(values).getValue());

        values = new TiffValue[]{new GeoTiffAscii("548"), new GeoTiffAscii("fdsd")};
        assertEquals(TiffType.ASCII.getValue(), TiffType.getType(values).getValue());

        values = new TiffValue[]{new TiffDouble(5486), new TiffDouble(86)};
        assertEquals(TiffType.DOUBLE.getValue(), TiffType.getType(values).getValue());
    }

    public void testGetType_WithValueNotSupported() {
        final TiffValue values[];
        values = new TiffValue[]{new TiffValueNotSupported()};

        try {
            TiffType.getType(values).getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().indexOf("not supported") != -1);
        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected, but was " + notExpected.getClass().getName());
        }
    }

    public void testGetType_WithDifferingValueTypes() {
        final TiffValue values[] = new TiffValue[]{
                new TiffRational(1, 1),
                new TiffLong(12345)
        };

        try {
            TiffType.getType(values).getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().indexOf("same type") != -1);
        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected, but was " + notExpected.getClass().getName());
        }

    }

    private static class TiffValueNotSupported extends TiffValue {

    }
}
