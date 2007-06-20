/*
 * $Id: SystemUtilsTest.java,v 1.5 2006/11/01 10:48:36 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.esa.beam.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.beam.GlobalTestConfig;

import java.io.File;
import java.io.IOException;
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

// Use this code to check out URL properties
//        System.out.println("url = " + url);
//        System.out.println("url.protocol = " + url.getProtocol());
//        System.out.println("url.path = " + url.getPath());
    }

    public void testGetApplicationHomeDirWithWindowsFileUrl() {

        String actualPath;

        if (File.separatorChar == '\\') {
            actualPath = getActualApplicationHomePath(
                    "jar:file:/C:\\Programme\\beam-3.2\\lib\\beam.jar!/org/esa/beam/visat/VisatMain.class");
            assertEquals("C:\\Programme\\beam-3.2", actualPath);

            actualPath = getActualApplicationHomePath(
                    "jar:file:/C:\\Programme\\theapp\\bin\\beam.jar!/org/esa/beam/visat/VisatMain.class");
            assertEquals("C:\\Programme\\theapp", actualPath);

            actualPath = getActualApplicationHomePath(
                    "jar:file:/C:\\Programme\\theapp\\beam.jar!/org/esa/beam/visat/VisatMain.class");
            assertEquals("C:\\Programme\\theapp", actualPath);

            actualPath = getActualApplicationHomePath(
                    "file:/C:\\Projects\\beam3\\classes\\org\\esa\\beam\\visat\\VisatMain.class");
            assertEquals("C:\\Projects\\beam3", actualPath);
        } else if (File.separatorChar == '/') {

            actualPath = getActualApplicationHomePath(
                    "jar:file:/usr/local/beam-3.2/lib/beam.jar!/org/esa/beam/visat/VisatMain.class");
            assertEquals("/usr/local/beam-3.2", actualPath);

            actualPath = getActualApplicationHomePath(
                    "jar:file:/usr/local/theapp/bin/beam.jar!/org/esa/beam/visat/VisatMain.class");
            assertEquals("/usr/local/theapp", actualPath);

            actualPath = getActualApplicationHomePath(
                    "jar:file:/usr/local/theapp/beam.jar!/org/esa/beam/visat/VisatMain.class");
            assertEquals("/usr/local/theapp", actualPath);

            actualPath = getActualApplicationHomePath(
                    "file:/home/norman/MyProjects/beam3/classes/org/esa/beam/visat/VisatMain.class");
            assertEquals("/home/norman/MyProjects/beam3", actualPath);
        } else {
            System.err.println("test not performed for File.separatorChar = '" + File.separatorChar + "'");
        }
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
        assertNotNull(SystemUtils.getApplicationHomeDir());
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

    public void testDeleteFileTree() {
        File treeRoot = GlobalTestConfig.getBeamTestDataOutputDirectory();
        File firstDir = new File(treeRoot, "firstDir");
        File firstFile = new File(treeRoot, "firstFile");
        File secondDir = new File(firstDir, "secondDir");
        File secondFile = new File(firstDir, "secondFile");
        File thirdFile = new File(secondDir, "thirdFile");
        File writeProtectedFile = new File(secondDir, "protFile");

        try {
            treeRoot.mkdirs();
            assertTrue(treeRoot.exists());

            firstDir.mkdirs();
            assertTrue(firstDir.exists());

            firstFile.createNewFile();
            assertTrue(firstFile.exists());

            secondDir.mkdirs();
            assertTrue(secondDir.exists());

            secondFile.createNewFile();
            assertTrue(secondFile.exists());

            thirdFile.createNewFile();
            assertTrue(thirdFile.exists());

            writeProtectedFile.createNewFile();
            assertTrue(writeProtectedFile.setReadOnly());
            assertTrue(writeProtectedFile.exists());

        } catch (IOException e) {
            fail("unable to create file");
        }

        SystemUtils.deleteFileTree(treeRoot);

        assertTrue(!treeRoot.exists());
        assertTrue(!firstDir.exists());
        assertTrue(!firstFile.exists());
        assertTrue(!secondDir.exists());
        assertTrue(!secondFile.exists());
        assertTrue(!thirdFile.exists());
        assertTrue(!writeProtectedFile.exists());
    }

    public void testDeleteFileTreeExceptions() {
        try {
            SystemUtils.deleteFileTree(null);
            fail("invalid null argument");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDeleteFileTree_DeleteEmptyDirectories() {
        File treeRoot = GlobalTestConfig.getBeamTestDataOutputDirectory();
        treeRoot.mkdirs();
        assertTrue("treeRoot exists expected", treeRoot.exists());
        File firstDir = new File(treeRoot, "firstDir");
        firstDir.mkdirs();
        assertTrue("firstDir exists expected", firstDir.exists());
        File emptyDir = new File(firstDir, "emptyDir");
        emptyDir.mkdirs();
        assertTrue("emptyDir exists expected", emptyDir.exists());

        SystemUtils.deleteFileTree(treeRoot);

        assertTrue("treeRoot not exists expected", !treeRoot.exists());
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
