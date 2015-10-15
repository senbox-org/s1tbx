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

package org.esa.snap.core.util;

import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * A collection of system level functions.
 */
public class SystemUtilsTest {

    @Test
    public void testApplicationProperties() {
        assertNotNull(SystemUtils.getApplicationContextId());
        assertFalse(SystemUtils.getApplicationContextId().isEmpty());

        assertNotNull(SystemUtils.getApplicationName());
        assertFalse(SystemUtils.getApplicationName().isEmpty());

        assertNotNull(SystemUtils.getApplicationHomepageUrl());
        assertFalse(SystemUtils.getApplicationHomepageUrl().isEmpty());

        assertNotNull(SystemUtils.getApplicationHomeDir());
    }

    @Test
    public void testClassFileName() {
        assertEquals("Date.class", SystemUtils.getClassFileName(java.util.Date.class));
        assertEquals("InputStream.class", SystemUtils.getClassFileName(java.io.InputStream.class));
        assertEquals("SystemUtils.class", SystemUtils.getClassFileName(SystemUtils.class));

        final URL url = SystemUtils.class.getResource(SystemUtils.getClassFileName(SystemUtils.class));
        assertNotNull(url);
        assertTrue("url = " + url.getPath(), url.getPath().endsWith("/org/esa/snap/core/util/SystemUtils.class"));
    }

    @Test
    public void testGetUserName() {
        assertNotNull(SystemUtils.getUserName());
    }

    @Test
    public void testGetUserHomeDir() {
        assertNotNull(SystemUtils.getUserHomeDir());
    }

    @Test
    public void testGetCurrentWorkingDir() {
        assertNotNull(SystemUtils.getCurrentWorkingDir());
    }

    @Test
    public void testGetClassPathFiles() {
        assertNotNull(SystemUtils.getClassPathFiles());
    }

    @Test
    public void testGetApplicationDataDir() {
        final File applicationDataDir = SystemUtils.getApplicationDataDir();
        assertNotNull(applicationDataDir);
        final String prefix = SystemUtils.getUserHomeDir().getPath();
        assertTrue(applicationDataDir.getPath().startsWith(prefix));
    }

    @Test
    public void testCreateHumanReadableExceptionMessage() {
        assertNull(SystemUtils.createHumanReadableExceptionMessage(null));

        assertNotNull(SystemUtils.createHumanReadableExceptionMessage(new Exception((String) null)));
        assertNotNull(SystemUtils.createHumanReadableExceptionMessage(new Exception("")));

        assertEquals("Heidewitzka, herr kapitän.",
                     SystemUtils.createHumanReadableExceptionMessage(new Exception("heidewitzka, herr kapitän")));

        assertEquals("Heidewitzka, herr kapitän.",
                     SystemUtils.createHumanReadableExceptionMessage(new Exception("heidewitzka, herr kapitän.")));

        assertEquals("Heidewitzka, herr kapitän!",
                     SystemUtils.createHumanReadableExceptionMessage(new Exception("heidewitzka, herr kapitän!")));
    }

    @Test
    public void testConvertPath() {
        String s = File.separator;
        String expected = s + "a" + s + "b" + s + "cdef" + s + "g";
        assertEquals(expected, SystemUtils.convertToLocalPath("/a/b/cdef/g"));
    }

}
