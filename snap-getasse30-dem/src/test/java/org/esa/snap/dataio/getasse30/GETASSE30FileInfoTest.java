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

package org.esa.snap.dataio.getasse30;

import junit.framework.TestCase;

import java.text.ParseException;


public class GETASSE30FileInfoTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }


    public void testValidFileSize() {
        assertTrue(GETASSE30FileInfo.isValidFileSize(2 * (1L * 1L)));
        assertTrue(GETASSE30FileInfo.isValidFileSize(2 * (2L * 2L)));
        assertTrue(GETASSE30FileInfo.isValidFileSize(2 * (3L * 3L)));
        assertTrue(GETASSE30FileInfo.isValidFileSize(2 * (4L * 4L)));
        assertTrue(GETASSE30FileInfo.isValidFileSize(2 * (5L * 5L)));
        assertTrue(GETASSE30FileInfo.isValidFileSize(2 * (1001L * 1001L)));
        assertTrue(GETASSE30FileInfo.isValidFileSize(2 * (6844L * 6844L)));

        assertFalse(GETASSE30FileInfo.isValidFileSize(-2));
        assertFalse(GETASSE30FileInfo.isValidFileSize(-1));
        assertFalse(GETASSE30FileInfo.isValidFileSize(0));
        assertFalse(GETASSE30FileInfo.isValidFileSize(1));
        assertFalse(GETASSE30FileInfo.isValidFileSize(3));
        assertFalse(GETASSE30FileInfo.isValidFileSize(4));
        assertFalse(GETASSE30FileInfo.isValidFileSize(5));
        assertFalse(GETASSE30FileInfo.isValidFileSize(6));
        assertFalse(GETASSE30FileInfo.isValidFileSize(7));
        assertFalse(GETASSE30FileInfo.isValidFileSize(9));
        assertFalse(GETASSE30FileInfo.isValidFileSize(87565));
        assertFalse(GETASSE30FileInfo.isValidFileSize(-76));
    }

    public void testExtractEastingNorthingWithValidStrings() throws ParseException {
        int[] values = GETASSE30FileInfo.parseEastingNorthing("00N015W.GETASSE30");
        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals(-15, values[0]);
        assertEquals(00, values[1]);

        values = GETASSE30FileInfo.parseEastingNorthing("75S135E.GETASSE30");
        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals(135, values[0]);
        assertEquals(-75, values[1]);
    }

    public void testExtractEastingNorthingWithInvalidStrings() {
        try {
            GETASSE30FileInfo.parseEastingNorthing("020n10w");  // string length  = 7
            fail("ParseException expected because the string not ends with '\\..+' ");
        } catch (ParseException expected) {
            assertEquals("Illegal string format.", expected.getMessage());
            assertEquals(7, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("005n104w"); // string length  = 8
            fail("ParseException expected because the string not ends with '\\..+' ");
        } catch (ParseException expected) {
            assertEquals("Illegal string format.", expected.getMessage());
            assertEquals(8, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("05S104E");
            fail("ParseException expected because the string not ends with '\\..+' ");
        } catch (ParseException expected) {
            assertEquals("Illegal string format.", expected.getMessage());
            assertEquals(7, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            // ok
        } catch (ParseException e) {
            fail("IllegalArgumentException expected");
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().indexOf("empty") > -1);
        } catch (ParseException e) {
            fail("IllegalArgumentException expected");
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("020n10aw");
            fail("ParseException expected because illegal 'a' character");
        } catch (ParseException expected) {
            assertEquals("Illegal direction character.", expected.getMessage());
            assertEquals(6, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("w0a0n10.GETASSE30");
            fail("ParseException expected because the string starts with no digit.");
        } catch (ParseException expected) {
            assertEquals("Digit character expected.", expected.getMessage());
            assertEquals(0, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("100n10w.GETASSE30");
            fail("ParseException expected because the value for north direction is out of bounds.");
        } catch (ParseException expected) {
            assertEquals("The value '100' for north direction is out of the range 0 ... 90.", expected.getMessage());
            assertEquals(3, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("100s10w.GETASSE30");
            fail("ParseException expected because the value for south direction is out of bounds.");
        } catch (ParseException expected) {
            assertEquals("The value '-100' for south direction is out of the range -90 ... 0.", expected.getMessage());
            assertEquals(3, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("80n190e.GETASSE30");
            fail("ParseException expected because the value for east direction is out of bounds.");
        } catch (ParseException expected) {
            assertEquals("The value '190' for east direction is out of the range 0 ... 180.", expected.getMessage());
            assertEquals(6, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("80s190w.GETASSE30");
            fail("ParseException expected because the value for west direction is out of bounds.");
        } catch (ParseException expected) {
            assertEquals("The value '-190' for west direction is out of the range -180 ... 0.", expected.getMessage());
            assertEquals(6, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("80s80s.GETASSE30");
            fail("ParseException expected because value for easting is not available");
        } catch (ParseException expected) {
            assertEquals("Easting value not available.", expected.getMessage());
            assertEquals(-1, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("80e80e.GETASSE30");
            fail("ParseException expected because value for northing is not available");
        } catch (ParseException expected) {
            assertEquals("Northing value not available.", expected.getMessage());
            assertEquals(-1, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("80e80sGETASSE30");
            fail("ParseException expected because northing easting values are not followed by a dot");
        } catch (ParseException expected) {
            assertEquals("Illegal string format.", expected.getMessage());
            assertEquals(6, expected.getErrorOffset());
        }

        try {
            GETASSE30FileInfo.parseEastingNorthing("80e80s.");
            fail("ParseException expected because the dot is not followed by at least one character");
        } catch (ParseException expected) {
            assertEquals("Illegal string format.", expected.getMessage());
            assertEquals(6, expected.getErrorOffset());
        }
    }
}
