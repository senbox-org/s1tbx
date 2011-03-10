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

package org.esa.beam.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A collection of system level functions.
 */
public class SystemUtilsTest extends TestCase {

    public SystemUtilsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(SystemUtilsTest.class);
    }

    @Override
    protected void tearDown() throws Exception {
        System.setProperty(SystemUtils.BEAM_PLUGIN_PATH_PROPERTY_NAME, "");
    }

    public void testClassFileName() {
        assertEquals("Date.class", SystemUtils.getClassFileName(java.util.Date.class));
        assertEquals("InputStream.class", SystemUtils.getClassFileName(java.io.InputStream.class));
        assertEquals("SystemUtils.class", SystemUtils.getClassFileName(SystemUtils.class));

        final URL url = SystemUtils.class.getResource(SystemUtils.getClassFileName(SystemUtils.class));
        assertNotNull(url);
        assertTrue("url = " + url.getPath(), url.getPath().endsWith("/org/esa/beam/util/SystemUtils.class"));
    }

    public void testGetUserName() {
        assertNotNull(SystemUtils.getUserName());
    }

    public void testGetUserHomeDir() {
        assertNotNull(SystemUtils.getUserHomeDir());
    }

    public void testGetCurrentWorkingDir() {
        assertNotNull(SystemUtils.getCurrentWorkingDir());
    }

    public void testGetClassPathFiles() {
        assertNotNull(SystemUtils.getClassPathFiles());
    }

    public void testGetApplicationHomeDir() {
        final File applicationHomeDir = SystemUtils.getApplicationHomeDir();
        assertNotNull(applicationHomeDir);
        assertTrue(applicationHomeDir.exists());
        assertTrue(new File(applicationHomeDir, "modules").exists());
        assertTrue(!applicationHomeDir.getPath().contains("modules"));
        assertTrue(!applicationHomeDir.getPath().contains(".jar"));
    }

    public void testGetApplicationDataDir() {
        final File applicationDataDir = SystemUtils.getApplicationDataDir();
        assertNotNull(applicationDataDir);
        final String prefix = SystemUtils.getUserHomeDir().getPath();
        assertTrue(applicationDataDir.getPath().startsWith(prefix));
    }

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

    public void testConvertPath() {
        String s = File.separator;
        String expected = s + "a" + s + "b" + s + "cdef" + s + "g";
        assertEquals(expected, SystemUtils.convertToLocalPath("/a/b/cdef/g"));
    }

    public void testGetBuildNumber() {
        String buildNumber = SystemUtils.getBuildNumber();
        assertNotNull("build number must be not null", buildNumber);
    }

    private static String getActualApplicationHomePath(final String spec) {
        final URL url;
        try {
            url = new URL(spec);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
        return SystemUtils.getApplicationHomeDir(url).getPath();
    }
}
