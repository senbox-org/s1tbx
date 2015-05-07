/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.ace;

import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.*;


public class TestACEFileInfo {

    @Test
    public void testValidFileSize() {
        assertTrue(ACEFileInfo.isValidFileSize(2 * (1L * 1L)));
        assertTrue(ACEFileInfo.isValidFileSize(2 * (2L * 2L)));
        assertTrue(ACEFileInfo.isValidFileSize(2 * (3L * 3L)));
        assertTrue(ACEFileInfo.isValidFileSize(2 * (4L * 4L)));
        assertTrue(ACEFileInfo.isValidFileSize(2 * (5L * 5L)));
        assertTrue(ACEFileInfo.isValidFileSize(2 * (1001L * 1001L)));
        assertTrue(ACEFileInfo.isValidFileSize(2 * (6844L * 6844L)));

        assertFalse(ACEFileInfo.isValidFileSize(-2));
        assertFalse(ACEFileInfo.isValidFileSize(-1));
        assertFalse(ACEFileInfo.isValidFileSize(0));
        assertFalse(ACEFileInfo.isValidFileSize(1));
        assertFalse(ACEFileInfo.isValidFileSize(3));
        assertFalse(ACEFileInfo.isValidFileSize(4));
        assertFalse(ACEFileInfo.isValidFileSize(5));
        assertFalse(ACEFileInfo.isValidFileSize(6));
        assertFalse(ACEFileInfo.isValidFileSize(7));
        assertFalse(ACEFileInfo.isValidFileSize(9));
        assertFalse(ACEFileInfo.isValidFileSize(87565));
        assertFalse(ACEFileInfo.isValidFileSize(-76));
    }

    @Test
    public void testExtractEastingNorthingWithValidStrings() throws ParseException {
        int[] values = ACEFileInfo.parseEastingNorthing("00N015W.ACE");
        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals(-15, values[0]);
        assertEquals(00, values[1]);

        values = ACEFileInfo.parseEastingNorthing("75S135E.ACE");
        assertNotNull(values);
        assertEquals(2, values.length);
        assertEquals(135, values[0]);
        assertEquals(-75, values[1]);
    }

    @Test
    public void testExtractEastingNorthingWithInvalidStrings() {
        try {
            ACEFileInfo.parseEastingNorthing("020n10w");  // string length  = 7
            fail("ParseException expected because the string not ends with '\\..+' ");
        } catch (ParseException expected) {
            assertEquals("Illegal string format.", expected.getMessage());
            assertEquals(7, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("005n104w"); // string length  = 8
            fail("ParseException expected because the string not ends with '\\..+' ");
        } catch (ParseException expected) {
            assertEquals("Illegal string format.", expected.getMessage());
            assertEquals(8, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("05S104E");
            fail("ParseException expected because the string not ends with '\\..+' ");
        } catch (ParseException expected) {
            assertEquals("Illegal string format.", expected.getMessage());
            assertEquals(7, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().indexOf("null") > -1);
        } catch (ParseException e) {
            fail("IllegalArgumentException expected");
        }

        try {
            ACEFileInfo.parseEastingNorthing("");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().indexOf("empty") > -1);
        } catch (ParseException e) {
            fail("IllegalArgumentException expected");
        }

        try {
            ACEFileInfo.parseEastingNorthing("020n10aw");
            fail("ParseException expected because illegal 'a' character");
        } catch (ParseException expected) {
            assertEquals("Illegal direction character.", expected.getMessage());
            assertEquals(6, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("w0a0n10.ACE");
            fail("ParseException expected because the string starts with no digit.");
        } catch (ParseException expected) {
            assertEquals("Digit character expected.", expected.getMessage());
            assertEquals(0, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("100n10w.ACE");
            fail("ParseException expected because the value for north direction is out of bounds.");
        } catch (ParseException expected) {
            assertEquals("The value '100' for north direction is out of the range 0 ... 90.", expected.getMessage());
            assertEquals(3, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("100s10w.ACE");
            fail("ParseException expected because the value for south direction is out of bounds.");
        } catch (ParseException expected) {
            assertEquals("The value '-100' for south direction is out of the range -90 ... 0.", expected.getMessage());
            assertEquals(3, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("80n190e.ACE");
            fail("ParseException expected because the value for east direction is out of bounds.");
        } catch (ParseException expected) {
            assertEquals("The value '190' for east direction is out of the range 0 ... 180.", expected.getMessage());
            assertEquals(6, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("80s190w.ACE");
            fail("ParseException expected because the value for west direction is out of bounds.");
        } catch (ParseException expected) {
            assertEquals("The value '-190' for west direction is out of the range -180 ... 0.", expected.getMessage());
            assertEquals(6, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("80s80s.ACE");
            fail("ParseException expected because value for easting is not available");
        } catch (ParseException expected) {
            assertEquals("Easting value not available.", expected.getMessage());
            assertEquals(-1, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("80e80e.ACE");
            fail("ParseException expected because value for northing is not available");
        } catch (ParseException expected) {
            assertEquals("Northing value not available.", expected.getMessage());
            assertEquals(-1, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("80e80sACE");
            fail("ParseException expected because northing easting values are not followed by a dot");
        } catch (ParseException expected) {
            assertEquals("Illegal string format.", expected.getMessage());
            assertEquals(6, expected.getErrorOffset());
        }

        try {
            ACEFileInfo.parseEastingNorthing("80e80s.");
            fail("ParseException expected because the dot is not followed by at least one character");
        } catch (ParseException expected) {
            assertEquals("Illegal string format.", expected.getMessage());
            assertEquals(6, expected.getErrorOffset());
        }
    }
}
