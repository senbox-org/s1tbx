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

package com.bc.ceres.core;

import static org.junit.Assert.*;

import org.junit.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.List;

public class VirtualDirTest {

    @Test
    public void testNestedZip() throws Exception {
        File zipFile = getTestDataDir("VirtualDirTestNested.zip");
        final VirtualDir virtualDir = VirtualDir.create(zipFile);
        assertNotNull(virtualDir);
        try {
            String[] allFileArray = virtualDir.listAllFiles();
            assertNotNull(allFileArray);
            List<String> allFiles = Arrays.asList(allFileArray);

            assertEquals(6, allFiles.size());
            assertTrue(allFiles.contains("dir1/File3"));
            assertTrue(allFiles.contains("dir1/File4"));
            assertTrue(allFiles.contains("dir1/dir3/File6"));
            assertTrue(allFiles.contains("File1"));
            assertTrue(allFiles.contains("File2"));
            assertTrue(allFiles.contains("File5.gz"));

            assertNotNull(virtualDir.getFile("File1"));
            assertNotNull(virtualDir.getFile("File2"));
            assertNotNull(virtualDir.getFile("File5.gz"));
            assertNotNull(virtualDir.getFile("dir1"));
            assertNotNull(virtualDir.getFile("dir1/"));
            assertNotNull(virtualDir.getFile("dir1/dir3"));
            assertNotNull(virtualDir.getFile("dir2"));
            assertNotNull(virtualDir.getFile("dir1/File3"));
            assertNotNull(virtualDir.getFile("dir1/File4"));
            assertNotNull(virtualDir.getFile("dir1/dir3/File6"));
        } finally {
            virtualDir.close();
        }
    }

    @Test
    public void testZip() throws IOException {
        File zipFile = getTestDataDir("VirtualDirTest.zip");
        final VirtualDir virtualDir = VirtualDir.create(zipFile);
        final File file1 = virtualDir.getFile("File1");
        assertTrue(file1.exists());
        assertFalse("Path of File1 should not contain the parent path of the zip file.",
                    file1.getAbsolutePath().contains(zipFile.getParent()));

        testFileNode(zipFile, virtualDir);

        assertTrue(virtualDir.isCompressed());
        assertTrue(virtualDir.isArchive());
    }

    @Test
    public void testDir() throws IOException {
        final File file = getTestDataDir("VirtualDirTest.dir");
        // make empty "dir2", because git removes empty dirs
        final File dir2 = new File(file, "dir2");
        dir2.mkdir();
        dir2.deleteOnExit();

        final VirtualDir virtualDir = VirtualDir.create(file);
        testFileNode(file, virtualDir);

        assertFalse(virtualDir.isCompressed());
        assertFalse(virtualDir.isArchive());
    }

    @Test
    public void testNoZip() throws IOException {
        File file = getTestDataDir("VirtualDirTest.nozip");
        VirtualDir virtualDir = VirtualDir.create(file);
        assertNull(virtualDir);
    }

    @Test
    public void testNullArg() throws IOException {
        try {
            VirtualDir.create(null);
            fail("NullPointerException?");
        } catch (NullPointerException e) {
            // ok
        }
    }

    @Test
    public void testFinalize() throws Throwable {
        File zipFile = getTestDataDir("VirtualDirTest.zip");
        final VirtualDir virtualDir = VirtualDir.create(zipFile);
        final File file1 = virtualDir.getFile("File1");   // triggers the unzipping

        final File tempDir = virtualDir.getTempDir();
        assertNotNull(tempDir);

        assertTrue(tempDir.isDirectory());

        virtualDir.finalize();

        assertFalse(tempDir.isDirectory());
    }

    @Test
    public void testFinalize_withSubdirectories() throws Throwable {
        File zipFile = getTestDataDir("VirtualDirTest.zip");
        final VirtualDir virtualDir = VirtualDir.create(zipFile);
        final File file3 = virtualDir.getFile("dir1/File3");   // triggers the unzipping

        final File tempDir = virtualDir.getTempDir();
        assertNotNull(tempDir);

        assertTrue(tempDir.isDirectory());

        virtualDir.finalize();

        assertFalse(tempDir.isDirectory());
    }

    private static File getTestDataDir() {
        File dir = new File("./src/test/data/");
        if (!dir.exists()) {
            dir = new File("./ceres-core/src/test/data/");
            if (!dir.exists()) {
                fail("Can't find my test data. Where is '" + dir + "'?");
            }
        }
        return dir;
    }

    private static File getTestDataDir(String path) {
        return new File(getTestDataDir(), path);
    }

    private void testFileNode(File file, VirtualDir virtualDir) throws IOException {
        assertNotNull(virtualDir);
        try {
            assertEquals(file.getPath(), virtualDir.getBasePath());
            assertNotNull(virtualDir.getFile("File1"));
            assertNotNull(virtualDir.getFile("File2"));
            assertNotNull(virtualDir.getFile("File5.gz"));
            assertNotNull(virtualDir.getFile("dir1"));
            assertNotNull(virtualDir.getFile("dir1/File3"));
            assertNotNull(virtualDir.getFile("dir1/File4"));
            assertNotNull(virtualDir.getFile("dir2"));

            assertTrue(virtualDir.exists("File2"));
            assertFalse(virtualDir.exists("nonExistentFile"));

            assertEquals(36, virtualDir.getFile("File5.gz").length());
            try (LineNumberReader reader = new LineNumberReader(virtualDir.getReader("File5.gz"))) {
                assertEquals("Norman was was was here!", reader.readLine());
            }

            List<String> dirNames = Arrays.asList(virtualDir.list("."));
            assertNotNull(dirNames);
            assertEquals(5, dirNames.size());
            assertTrue(dirNames.contains("dir1"));
            assertTrue(dirNames.contains("dir2"));
            assertTrue(dirNames.contains("File1"));
            assertTrue(dirNames.contains("File2"));
            assertTrue(dirNames.contains("File5.gz"));

            dirNames = Arrays.asList(virtualDir.list(""));
            assertNotNull(dirNames);
            assertEquals(5, dirNames.size());
            assertTrue(dirNames.contains("dir1"));
            assertTrue(dirNames.contains("dir2"));
            assertTrue(dirNames.contains("File1"));
            assertTrue(dirNames.contains("File2"));
            assertTrue(dirNames.contains("File5.gz"));

            // todo - test other cases with same result, e.g. "dir1/.."

            List<String> dir1Names = Arrays.asList(virtualDir.list("dir1"));
            assertNotNull(dir1Names);
            assertEquals(2, dir1Names.size());
            assertTrue(dir1Names.contains("File3"));
            assertTrue(dir1Names.contains("File4"));

            // todo - test other cases with same result, e.g. "./dir1"

            String[] dir2Names = virtualDir.list("dir2");
            assertNotNull(dir2Names);
            assertEquals(0, dir2Names.length);

            try {
                virtualDir.list("dir3");
                fail("IOException?");
            } catch (FileNotFoundException ignored) {
                // ok
            }
            try {
                virtualDir.getFile("File3");
                fail("IOException?");
            } catch (FileNotFoundException ignored) {
                // ok
            }
            try {
                virtualDir.getFile("dir1/File1");
                fail("IOException?");
            } catch (FileNotFoundException ignored) {
                // ok
            }

            String[] allFileArray = virtualDir.listAllFiles();
            assertNotNull(allFileArray);
            List<String> allFiles = Arrays.asList(allFileArray);
            assertEquals(5, allFiles.size());
            assertTrue(allFiles.contains("dir1/File3"));
            assertTrue(allFiles.contains("dir1/File4"));
            assertTrue(allFiles.contains("File1"));
            assertTrue(allFiles.contains("File2"));
            assertTrue(allFiles.contains("File5.gz"));
        } finally {
            virtualDir.close();
        }
    }
}
