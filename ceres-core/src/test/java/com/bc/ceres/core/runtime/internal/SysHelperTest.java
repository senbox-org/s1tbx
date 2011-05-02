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

import static com.bc.ceres.core.runtime.internal.SysHelper.*;
import static org.junit.Assert.*;


public class SysHelperTest {
    @Test
    public void testGetExt() throws Exception {
        assertEquals(".dll", SysHelper.getExt("banana.dll"));
        assertEquals(".so", SysHelper.getExt(".beam/lib/banana.so"));
        assertEquals(".jnilib", SysHelper.getExt("/banana.jnilib"));
        assertEquals(null, SysHelper.getExt("banana.so/readme"));
        assertEquals(null, SysHelper.getExt(".so"));
        assertEquals(null, SysHelper.getExt("/opt/.so"));
    }

    @Test
    public void testIsNativeFileName() throws Exception {
        assertEquals(true, SysHelper.isNativeFileName("banana.so"));
        assertEquals(true, SysHelper.isNativeFileName("banana.jnilib"));
        assertEquals(true, SysHelper.isNativeFileName("banana.dll"));
        assertEquals(false, SysHelper.isNativeFileName("banana.txt"));
        assertEquals(false, SysHelper.isNativeFileName("readme"));
    }

    @Test
    public void testPlatformIdNamesAreAllLowerCase() throws Exception {
        for (PlatformId platformId : PlatformId.values()) {
            assertEquals(platformId.toString().toLowerCase(), platformId.toString());
        }
    }

    @Test
    public void testToPlatformId() throws Exception {
        assertEquals(PlatformId.linux, toPlatformId("Linux"));
        assertEquals(PlatformId.macosx, toPlatformId("Mac OS X"));
        assertEquals(PlatformId.win, toPlatformId("Windows 7"));
        assertEquals(PlatformId.win, toPlatformId("Windows Vista"));
        assertEquals(null, toPlatformId("Vindows Wista"));
        try {
            toPlatformId(null);
            fail("NPE?");
        } catch (NullPointerException e) {
        }
    }


}
