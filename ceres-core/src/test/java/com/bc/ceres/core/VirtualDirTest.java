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

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class VirtualDirTest extends TestCase {

    public void testZip() throws IOException {
        File zipFile = get("FileNodeTest.zip");
        final VirtualDir virtualDir = VirtualDir.create(zipFile);
        final File file1 = virtualDir.getFile("File1");
        assertTrue(file1.exists());
        assertFalse("Path of File1 should not contain the parent path of the zip file.",
                    file1.getAbsolutePath().contains(zipFile.getParent()));

        testFileNode(zipFile, virtualDir);
    }

    public void testDir() throws IOException {
        File file = get("FileNodeTest.dir");
        File dir2 = new File(file, "dir2");
        dir2.mkdir();
        dir2.deleteOnExit();
        testFileNode(file, VirtualDir.create(file));
    }

    private void testFileNode(File file, VirtualDir virtualDir) throws IOException {
        assertNotNull(virtualDir);
        try {
            assertEquals(file.getPath(), virtualDir.getBasePath());
            assertNotNull(virtualDir.getFile("File1"));
            assertNotNull(virtualDir.getFile("File2"));
            assertNotNull(virtualDir.getFile("dir1"));
            assertNotNull(virtualDir.getFile("dir1/File3"));
            assertNotNull(virtualDir.getFile("dir1/File4"));
            assertNotNull(virtualDir.getFile("dir2"));

            List<String> dir1Names = Arrays.asList(virtualDir.list("dir1"));
            assertNotNull(dir1Names);
            assertEquals(2, dir1Names.size());
            assertTrue(dir1Names.contains("File3"));
            assertTrue(dir1Names.contains("File4"));

            String[] dir2Names = virtualDir.list("dir2");
            assertNotNull(dir2Names);
            assertEquals(0, dir2Names.length);

            try {
                virtualDir.list("dir3");
                fail("IOException?");
            } catch (FileNotFoundException e) {
                // ok
            }
            try {
                virtualDir.getFile("File3");
                fail("IOException?");
            } catch (FileNotFoundException e) {
                // ok
            }
            try {
                virtualDir.getFile("dir1/File1");
                fail("IOException?");
            } catch (FileNotFoundException e) {
                // ok
            }
        } finally {
            virtualDir.close();
        }
    }

    public void testNoZip() throws IOException {
        File file = get("FileNodeTest.nozip");
        VirtualDir virtualDir = VirtualDir.create(file);
        assertNull(virtualDir);
    }


    public void testNullArg() throws IOException {
        try {
            VirtualDir.create(null);
            fail("NullPointerException?");
        } catch (NullPointerException e) {
            // ok
        }
    }

    private static File get() {
        File dir = new File("./src/test/data/");
        if (!dir.exists()) {
            dir = new File("./ceres-core/src/test/data/");
            if (!dir.exists()) {
                junit.framework.Assert.fail("Can't find my test data. Where is '" + dir + "'?");
            }
        }
        return dir;
    }

    private static File get(String path) {
        return new File(get(), path);
    }

}
