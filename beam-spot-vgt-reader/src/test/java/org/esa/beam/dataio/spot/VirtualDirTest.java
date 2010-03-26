package org.esa.beam.dataio.spot;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class VirtualDirTest extends TestCase {

    public void testZip() throws IOException {
        File file = TestDataDir.get("FileNodeTest.zip");
        testFileNode(file, VirtualDir.create(file));
    }

    public void testDir() throws IOException {
        File file = TestDataDir.get("FileNodeTest.dir");
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

            String[] dir1Names = virtualDir.list("dir1");
            assertNotNull(dir1Names);
            assertEquals(2, dir1Names.length);
            assertEquals("File3", dir1Names[0]);
            assertEquals("File4", dir1Names[1]);

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
        File file = TestDataDir.get("FileNodeTest.nozip");
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
}
