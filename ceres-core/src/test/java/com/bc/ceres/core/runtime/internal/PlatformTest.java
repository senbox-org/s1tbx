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

package com.bc.ceres.core.runtime.internal;

import org.junit.Test;

import static org.junit.Assert.*;


public class PlatformTest {

    @Test
    public void testPlatform() throws Exception {
        Platform win64 = new Platform(Platform.ID.win, 64);
        assertEquals(Platform.ID.win, win64.getId());
        assertEquals(64, win64.getBitCount());
        assertTrue(win64.isPlatformDir("lib/win64/test.dll"));
        assertFalse(win64.isPlatformDir("lib/win32/test.dll"));
        assertFalse(win64.isPlatformDir("lib/unix64/test.dll"));

        assertEquals("lib/test.dll", win64.truncatePlatformDir("lib/win64/test.dll"));
        assertEquals("lib/linux64/test.so", win64.truncatePlatformDir("lib/linux64/test.so"));
    }

    @Test
    public void testIsAnyPlatformDir() throws Exception {
        assertTrue(Platform.isAnyPlatformDir("lib/win32/readme"));
        assertTrue(Platform.isAnyPlatformDir("lib/win32/test.dll"));
        assertTrue(Platform.isAnyPlatformDir("lib/win64/test.dll"));
        assertTrue(Platform.isAnyPlatformDir("lib/linux32/readme"));
        assertTrue(Platform.isAnyPlatformDir("lib/linux32/test.so"));
        assertTrue(Platform.isAnyPlatformDir("lib/linux32/test.so"));
        assertTrue(Platform.isAnyPlatformDir("lib/macosx32/readme"));
        assertTrue(Platform.isAnyPlatformDir("lib/macosx32/test.jnilib"));
        assertTrue(Platform.isAnyPlatformDir("lib/macosx64/test.jnilib"));

        assertFalse(Platform.isAnyPlatformDir("win32/test.dll"));
        assertFalse(Platform.isAnyPlatformDir("linux32/readme"));
        assertFalse(Platform.isAnyPlatformDir("macosx64/test.jnilib"));
    }

    @Test
    public void testPlatformIdNamesAreAllLowerCase() throws Exception {
        for (Platform.ID platformId : Platform.ID.values()) {
            assertEquals(platformId.toString().toLowerCase(), platformId.toString());
        }
    }

    @Test
    public void testToPlatformId() throws Exception {
        assertEquals(Platform.ID.linux, Platform.getPlatformId("Linux"));
        assertEquals(Platform.ID.macosx, Platform.getPlatformId("Mac OS X"));
        assertEquals(Platform.ID.win, Platform.getPlatformId("Windows 7"));
        assertEquals(Platform.ID.win, Platform.getPlatformId("Windows Vista"));
        assertEquals(null, Platform.getPlatformId("Vindows Wista"));
        try {
            Platform.getPlatformId(null);
            fail("NPE?");
        } catch (NullPointerException e) {
        }
    }


}
