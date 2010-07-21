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

package org.esa.beam.dataio.ceos;


import junit.framework.TestCase;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CeosFileReaderTest extends TestCase {

    private MemoryCacheImageOutputStream _ios;

    @Override
    protected void setUp() throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        _ios = new MemoryCacheImageOutputStream(os);
    }

    public void testSeek() throws IOException,
                                  IllegalCeosFormatException {
        final byte[] bytes = new byte[]{
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
        };
        _ios.write(bytes);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(1);
        assertEquals(1, ceosReader.readB1());
        ceosReader.seek(3);
        assertEquals(3, ceosReader.readB1());
        ceosReader.seek(9);
        assertEquals(9, ceosReader.readB1());
        ceosReader.seek(4);
        assertEquals(4, ceosReader.readB1());
        ceosReader.seek(14);
        assertEquals(14, ceosReader.readB1());
    }

    public void testSkipBytes() throws IOException,
                                       IllegalCeosFormatException {
        final byte[] bytes = new byte[]{
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
        };
        _ios.write(bytes);
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.skipBytes(1);
        assertEquals(1, ceosReader.readB1());
        ceosReader.skipBytes(3);
        assertEquals(5, ceosReader.readB1());
        ceosReader.skipBytes(5);
        assertEquals(11, ceosReader.readB1());
    }

    public void testReadB1() throws IOException, IllegalCeosFormatException {
        _ios.writeByte(122);
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(122, ceosReader.readB1());
    }

    public void testReadB1GreatValue() throws IOException,
                                              IllegalCeosFormatException {
        _ios.writeByte(245);
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(245, ceosReader.readB1());
    }

    public void testReadB1ThrowsException() throws IOException {
        final String prefix = "ddz716d51n+dn4drh1td6r4nh64n1687";
        _ios.writeBytes(prefix);
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        try {
            ceosReader.readB1();
            fail("IllegalCeosFormatException expected");
        } catch (IllegalCeosFormatException e) {
            assertEquals(prefix.length(), e.getStreamPos());
        }
    }

    public void testReadB2() throws IOException, IllegalCeosFormatException {
        final String prefix = "ß3534aß0uawemqw34mfavsdpvhaweföldv:";
        final String suffix = "lfjldfkjvg45";
        final short expected = -12354;
        _ios.writeBytes(prefix);
        _ios.writeShort(expected);
        _ios.writeBytes(suffix);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        assertEquals(expected, ceosReader.readB2());
    }

    public void testReadB4() throws IOException, IllegalCeosFormatException {
        final String prefix = "ß3534aß0uawemqw34mfavsdpvhaweföldv:";
        final String suffix = "lfjldfkjvg45";
        _ios.writeBytes(prefix);
        _ios.writeInt(7100);
        _ios.writeBytes(suffix);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        assertEquals(7100, ceosReader.readB4());
    }

    public void testReadB4Array() throws IOException, IllegalCeosFormatException {
        final String prefix = "gf654hdf4f46514s:";
        final String suffix = "lfjldfkjvg45";
        final int expected1 = 7100;
        final int expected2 = -98769342;
        _ios.writeBytes(prefix);
        _ios.writeInt(expected1);
        _ios.writeInt(expected2);
        _ios.writeBytes(suffix);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        final int[] intsToRead = new int[2];
        ceosReader.readB4(intsToRead);
        assertEquals(expected1, intsToRead[0]);
        assertEquals(expected2, intsToRead[1]);
    }

    public void testReadB4ArrayThrowsException() throws IOException {
        final String prefix = "ddz716d51n+dn4drh1td6r4nh64n1687";
        _ios.writeBytes(prefix);
        _ios.writeInt(234521431);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        final int[] intsToRead = new int[2];
        try {
            ceosReader.readB4(intsToRead);
            fail("IllegalCeosFormatException expected");
        } catch (IllegalCeosFormatException e) {
            assertEquals(prefix.length(), e.getStreamPos());
        }
    }

    public void testReadB8() throws IOException,
                                    IllegalCeosFormatException {
        final byte[] bytes = new byte[]{0x00, 0x01, 0x00, 0x11, 0x00, 0x00, 0x1B, (byte) 0xBC};
        _ios.write(bytes);
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(281547991161788L, ceosReader.readB8());
    }

    public void testReadB8Array() throws IOException,
                                         IllegalCeosFormatException {
        final long expected1 = 281547991161788L;
        final long expected2 = 1L;
        _ios.writeLong(expected1);
        _ios.writeLong(expected2);
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        final long[] values = new long[2];
        ceosReader.readB8(values);
        assertEquals(expected1, values[0]);
        assertEquals(expected2, values[1]);
    }

    public void testReadB1Array() throws IOException,
                                         IllegalCeosFormatException {
        final byte expected1 = 0x01;
        final byte expected2 = 0x02;
        final byte expected3 = 0x03;
        final byte expected4 = 0x04;
        final byte expected5 = 0x1B;
        final byte expected6 = (byte) 246;
        final byte expected7 = 0x09;
        final byte expected8 = 0x0b;
        _ios.write(expected1);
        _ios.write(expected2);
        _ios.write(expected3);
        _ios.write(expected4);
        _ios.write(expected5);
        _ios.write(expected6);
        _ios.write(expected7);
        _ios.write(expected8);
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        final byte[] values = new byte[8];
        ceosReader.readB1(values);
        assertEquals(expected1, values[0]);
        assertEquals(expected2, values[1]);
        assertEquals(expected3, values[2]);
        assertEquals(expected4, values[3]);
        assertEquals(expected5, values[4]);
        assertEquals(expected6, values[5]);
        assertEquals(expected7, values[6]);
        assertEquals(expected8, values[7]);
    }

    public void testReadI4() throws IllegalCeosFormatException, IOException {
        _ios.writeBytes("19730060");
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(1973, ceosReader.readI4());
        assertEquals(60, ceosReader.readI4());
    }

    public void testReadIn() throws IllegalCeosFormatException, IOException {
        _ios.writeBytes("  7358");
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(7358, ceosReader.readIn(6));
    }

    public void testReadFnWithNegative() throws IllegalCeosFormatException, IOException {
        _ios.writeBytes("     -89.0060123");
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(-89.0060123, ceosReader.readFn(16), 1E-10);
    }

    public void testReadFnWithPositive() throws IllegalCeosFormatException, IOException {
        _ios.writeBytes("      19.0060123");
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(19.0060123, ceosReader.readFn(16), 1E-10);
    }

    public void testReadFnWithLeadingZero() throws IllegalCeosFormatException, IOException {
        _ios.writeBytes("       9.0060123");
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(9.0060123, ceosReader.readFn(16), 1E-10);
    }

    public void testReadFnWithTrailingZero() throws IllegalCeosFormatException, IOException {
        _ios.writeBytes("       9.0060000");
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(9.006, ceosReader.readFn(16), 1E-6);
    }

    public void testReadFnWithEmpty() throws IllegalCeosFormatException,
                                             IOException {
        final String prefix = "iufkdjker";
        final String value = "                ";
        final String suffix = "oiunkasdjnf";
        _ios.writeBytes(prefix);
        _ios.writeBytes(value);
        _ios.writeBytes(suffix);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        try {
            ceosReader.readFn(16);
            fail("IllegalCeosFormatException expected");
        } catch (IllegalCeosFormatException e) {
            assertEquals(prefix.length(), e.getStreamPos());
        }

    }

    public void testReadEn() throws IllegalCeosFormatException, IOException {
        _ios.writeBytes(" 1.782000000000000E+04");
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(17820, ceosReader.readEn(22), 1E-6);
    }

    public void testReadGn() throws IllegalCeosFormatException, IOException {
        _ios.writeBytes("-1.06962770630708111E+01");
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(-1.06962770630708111E+01, ceosReader.readGn(24), 1E-25);
    }

    public void testReadGnArray() throws IllegalCeosFormatException,
                                         IOException {
        _ios.writeBytes("-1.06962770630708111E+01");
        _ios.writeBytes(" 1.28993192035406507E-05");
        _ios.writeBytes("-8.94946528898421729E-05");
        _ios.writeBytes(" 6.75271499535523411E-13");
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        final double[] values = new double[4];
        ceosReader.readGn(24, values);
        assertEquals(-1.06962770630708111E+01, values[0], 1e-25);
        assertEquals(1.28993192035406507E-05, values[1], 1e-25);
        assertEquals(-8.94946528898421729E-05, values[2], 1e-25);
        assertEquals(6.75271499535523411E-13, values[3], 1e-25);
    }

    public void testReadAn() throws IllegalCeosFormatException, IOException {
        final String expected = "Kinkerlitzchen";
        _ios.writeBytes(expected);
        _ios.seek(0);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        assertEquals(expected, ceosReader.readAn(expected.length()));
    }

    public void testReadB1ArrayThrowsException() throws IOException {
        final String prefix = "ddz716d51n+dn4drh1td6r4nh64n1687";
        _ios.writeBytes(prefix);
        _ios.write(new byte[]{0x00, (byte) 0xBC, 0x00, 0x01});

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        final byte[] bytesToRead = new byte[8]; // only for bytes written
        try {
            ceosReader.readB1(bytesToRead);
            fail("IllegalCeosFormatException expected");
        } catch (IllegalCeosFormatException e) {
            assertEquals(prefix.length(), e.getStreamPos());
        }
    }

    public void testReadAnThrowsExceptionBecauseStreamIsToShort() throws IOException {
        final String prefix = "dflkjoieng nvivbaewr vpivbydv";
        final String charsToRead = "To lon"; // write 6 bytes to stream
        _ios.writeBytes(prefix);
        _ios.writeBytes(charsToRead);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        try {
            ceosReader.readAn(7); // try to read 7 bytes from stream position
            fail("IllegalCeosFormatException expected");
        } catch (IllegalCeosFormatException e) {
            assertEquals(prefix.length(), e.getStreamPos());
        }
    }

    public void testReadFnWithExceptionBecauseStreamIsToShort() throws IOException {
        final String prefix = "following only 15 characters but it should read 16: ";
        final String only15Characters = "123456789.12345";
        _ios.writeBytes(prefix);
        _ios.writeBytes(only15Characters);

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        try {
            ceosReader.readFn(16);
            fail("IllegalCeosFormatException expected");
        } catch (IllegalCeosFormatException e) {
            assertEquals(prefix.length(), e.getStreamPos());
        }
    }

    public void testReadFnWithExceptionBecauseDoubleIsNotParsable() throws IOException {
        final String prefix = "following a not parsable double value : ";
        final String notParsable16Double = "1234g6789.123456";
        _ios.writeBytes(prefix);
        _ios.writeBytes(notParsable16Double);
        _ios.writeBytes("suffix letters");

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        try {
            ceosReader.readFn(16);
            fail("IllegalCeosFormatException expected");
        } catch (IllegalCeosFormatException e) {
            assertEquals(prefix.length(), e.getStreamPos());
        }
    }

    public void testReadInArray() throws IOException, IllegalCeosFormatException {
        final String prefix = "vspdfoperilfdkposnsern";
        _ios.writeBytes(prefix);
        _ios.writeBytes("123"); // 0
        _ios.writeBytes(" 45"); // 1
        _ios.writeBytes("  6"); // 2
        _ios.writeBytes(" 46"); // 3
        _ios.writeBytes(" 7 "); // 4
        _ios.writeBytes("234"); // 5
        _ios.writeBytes("suffix");

        final CeosFileReader ceosReader = new CeosFileReader(_ios);
        ceosReader.seek(prefix.length());
        final int[] ints = ceosReader.readInArray(6, 3);

        assertNotNull(ints);
        assertEquals(6, ints.length);
        assertEquals(123, ints[0]);
        assertEquals(45, ints[1]);
        assertEquals(6, ints[2]);
        assertEquals(46, ints[3]);
        assertEquals(7, ints[4]);
        assertEquals(234, ints[5]);
        assertEquals(prefix.length() + 6 * 3, _ios.getStreamPosition());
    }

    public void testReadInArrayWithBlanks() throws IOException, IllegalCeosFormatException {
        final String prefix = "vspdfoperilfdkposnsern";
        _ios.writeBytes(prefix);
        _ios.writeBytes("123 45 67"); // 9 ints with length 1
        _ios.writeBytes("suffix");

        final CeosFileReader ceosReader = new CeosFileReader(_ios);
        ceosReader.seek(prefix.length());
        final int[] ints = ceosReader.readInArray(9, 1);

        assertNotNull(ints);
        assertEquals(9, ints.length);
        assertEquals(1, ints[0]);
        assertEquals(2, ints[1]);
        assertEquals(3, ints[2]);
        assertEquals(0, ints[3]);
        assertEquals(4, ints[4]);
        assertEquals(5, ints[5]);
        assertEquals(0, ints[6]);
        assertEquals(6, ints[7]);
        assertEquals(7, ints[8]);
        assertEquals(prefix.length() + 9 * 1, _ios.getStreamPosition());
    }

    public void testReadInArrayWithErrors() throws IOException {
        final String prefix = "vspdfoperilfdkposnsern";
        _ios.writeBytes(prefix);
        _ios.writeBytes("123a45"); // 6 ints with length 1
        _ios.writeBytes("suffix");

        final CeosFileReader ceosReader = new CeosFileReader(_ios);

        ceosReader.seek(prefix.length());
        try {
            ceosReader.readInArray(6, 1);
            fail("IllegalCeosFormatException expected");
        } catch (IllegalCeosFormatException e) {
            assertEquals(prefix.length() + 3, e.getStreamPos());
        }
    }
}