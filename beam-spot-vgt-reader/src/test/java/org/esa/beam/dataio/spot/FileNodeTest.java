package org.esa.beam.dataio.spot;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileNodeTest extends TestCase {

    public void testZip() throws IOException {
        File file = TestDataDir.get("FileNodeTest.zip");
        testFileNode(file, FileNode.create(file));
    }

    public void testDir() throws IOException {
        File file = TestDataDir.get("FileNodeTest.dir");
        testFileNode(file, FileNode.create(file));
    }

    private void testFileNode(File file, FileNode fileNode) throws IOException {
        assertNotNull(fileNode);
        try {
            assertEquals(file.getPath(), fileNode.getName());
            assertNotNull(fileNode.getFile("File1"));
            assertNotNull(fileNode.getFile("File2"));
            assertNotNull(fileNode.getFile("dir1/File3"));
            assertNotNull(fileNode.getFile("dir1/File4"));

            String[] dir1Names = fileNode.list("dir1");
            assertNotNull(dir1Names);
            assertEquals(2, dir1Names.length);
            assertEquals("File3", dir1Names[0]);
            assertEquals("File4", dir1Names[1]);

            try {
                fileNode.list("dir2");
                fail("IOException?");
            } catch (FileNotFoundException e) {
                // ok
            }
            try {
                fileNode.getFile("File3");
                fail("IOException?");
            } catch (FileNotFoundException e) {
                // ok
            }
            try {
                fileNode.getFile("dir1/File1");
                fail("IOException?");
            } catch (FileNotFoundException e) {
                // ok
            }
        } finally {
            fileNode.close();
        }
    }

    public void testNoZip() throws IOException {
        File file = TestDataDir.get("FileNodeTest.nozip");
        FileNode fileNode = FileNode.create(file);
        assertNull(fileNode);
    }


    public void testNullArg() throws IOException {
        try {
            FileNode.create(null);
            fail("NullPointerException?");
        } catch (NullPointerException e) {
            // ok
        }
    }
}
