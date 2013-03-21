package org.esa.beam.dataio.landsat.tgz;

import org.esa.beam.dataio.landsat.TestUtil;
import org.esa.beam.util.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class VirtualDirTgzTest {

    private VirtualDirTgz virtualDir;

    @Test
    public void testOpenTgz() throws IOException {
        File testTgz = TestUtil.getTestFile("tgz/test-archive.tgz");

        virtualDir = new VirtualDirTgz(testTgz);
        assertEquals(testTgz.getPath(), virtualDir.getBasePath());

        assertTrue(virtualDir.isCompressed());
        assertTrue(virtualDir.isArchive());
    }

    @Test
    public void testOpenTar() throws IOException {
        File testTar = TestUtil.getTestFile("tgz/test-archive.tar");

        virtualDir = new VirtualDirTgz(testTar);
        assertEquals(testTar.getPath(), virtualDir.getBasePath());

        assertFalse(virtualDir.isCompressed());
        assertTrue(virtualDir.isArchive());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Test
    public void testOpenNull() throws IOException {
        try {
            final VirtualDirTgz vdTgz = new VirtualDirTgz(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testTar_getInputStream() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tar");

        virtualDir = new VirtualDirTgz(testTgz);
        assertExpectedInputStream();
    }

    @Test
    public void testTgz_getInputStream() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tgz");

        virtualDir = new VirtualDirTgz(testTgz);
        assertExpectedInputStream();
    }

    @Test
    public void testTar_getInputStream_invalidPath() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tar");

        virtualDir = new VirtualDirTgz(testTgz);
        assertInputStreamInvalidPath();
    }

    @Test
    public void testTgz_getInputStream_invalidPath() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tgz");

        virtualDir = new VirtualDirTgz(testTgz);
        assertInputStreamInvalidPath();
    }

    @Test
    public void testTar_getFile() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tar");

        virtualDir = new VirtualDirTgz(testTgz);
        assertExpectedFile();
    }

    @Test
    public void testTgz_getFile() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tgz");

        virtualDir = new VirtualDirTgz(testTgz);
        assertExpectedFile();
    }

    @Test
    public void testTar_noDirInTar_getFile() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive_wo_dir.tar");

        virtualDir = new VirtualDirTgz(testTgz);
        final File file_1 = virtualDir.getFile("file1.txt");
        assertNotNull(file_1);
    }

    @Test
    public void testTar_getFile_invalidPath() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tar");

        virtualDir = new VirtualDirTgz(testTgz);
        assertGetFileInvalidPath();
    }

    @Test
    public void testTgz_getFile_invalidPath() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tgz");

        virtualDir = new VirtualDirTgz(testTgz);
        assertGetFileInvalidPath();
    }

    @Test
    public void testTar_list() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tar");

        virtualDir = new VirtualDirTgz(testTgz);
        assertCorrectList();
    }

    @Test
    public void testTgz_list() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tgz");

        virtualDir = new VirtualDirTgz(testTgz);
        assertCorrectList();
    }

    @Test
    public void testTar_list_invalidPath() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tar");

        virtualDir = new VirtualDirTgz(testTgz);
        assertListInvalidPath();
    }

    @Test
    public void testTgz_list_invalidPath() throws IOException {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tgz");

        virtualDir = new VirtualDirTgz(testTgz);
        assertListInvalidPath();
    }

    @Test
    public void testFinalize() throws Throwable {
        final File testTgz = TestUtil.getTestFile("tgz/test-archive.tgz");

        virtualDir = new VirtualDirTgz(testTgz);
        assertExpectedFile();

        final File tempDir = virtualDir.getTempDir();
        assertNotNull(tempDir);
        
        assertTrue(tempDir.isDirectory());

        virtualDir.finalize();
        assertFalse(tempDir.isDirectory());
    }

    @Test
    public void testGetFilenameFromPath_Windows() {
        final String fullPath = "C:\\bla\\blubber\\theFile.txt";
        assertEquals("theFile.txt", VirtualDirTgz.getFilenameFromPath(fullPath));

        final String relativePath = "bla\\schnuffi\\schnatter.txt";
        assertEquals("schnatter.txt", VirtualDirTgz.getFilenameFromPath(relativePath));
    }

    @Test
    public void testGetFilenameFromPath_Linux() {
        final String fullPath = "/bla/blubber/theFile.txt";
        assertEquals("theFile.txt", VirtualDirTgz.getFilenameFromPath(fullPath));

        final String relativePath = "bla/schnuffi/schnatter.txt";
        assertEquals("schnatter.txt", VirtualDirTgz.getFilenameFromPath(relativePath));
    }

    @Test
    public void testGetFilenameFromPath_notAPath() {
        final String file = "theFile.txt";
        assertEquals(file, VirtualDirTgz.getFilenameFromPath(file));
    }

    @Test
    public void testIsTgz() {
        assertTrue(VirtualDirTgz.isTgz("test_archive.tar.gz"));
        assertTrue(VirtualDirTgz.isTgz("test_archive.tgz"));

        assertFalse(VirtualDirTgz.isTgz("test_archive.tar"));
        assertFalse(VirtualDirTgz.isTgz("test_archive.exe"));
        assertFalse(VirtualDirTgz.isTgz("test_archive"));
    }

    @After
    public void tearDown() {
        if (virtualDir != null) {
            virtualDir.close();
        }
    }

    private void assertExpectedInputStream() throws IOException {
        final InputStream inputStream = virtualDir.getInputStream("test-archive/file2.txt");
        try {
            assertNotNull(inputStream);

            final byte[] buffer = new byte[512];
            int bytesRead = inputStream.read(buffer);
            assertEquals(9, bytesRead);
            assertEquals("content2", new String(buffer).trim());
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void assertInputStreamInvalidPath() {
        try {
            virtualDir.getInputStream("test-archive/invalid_dir/no.file");
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }

    private void assertExpectedFile() throws IOException {
        final File file = virtualDir.getFile("test-archive/dir1/file3.txt");
        assertNotNull(file);
        assertTrue((file.isFile()));

        final FileInputStream inputStream = new FileInputStream(file);
        try {
            final byte[] buffer = new byte[512];
            int bytesRead = inputStream.read(buffer);
            assertEquals(9, bytesRead);
            assertEquals("content3", new String(buffer).trim());
        } finally {
            inputStream.close();
        }
    }

    private void assertGetFileInvalidPath() {
        try {
            virtualDir.getFile("test-archive/invalid_dir/missing.file");
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }

    private void assertCorrectList() throws IOException {
        String[] list = virtualDir.list("");
        List<String> dirList = Arrays.asList(list);
        assertEquals(1, dirList.size());
        assertTrue(dirList.contains("test-archive"));

        list = virtualDir.list("test-archive");
        dirList = Arrays.asList(list);
        assertEquals(3, dirList.size());
        assertTrue(dirList.contains("dir1"));
        assertTrue(dirList.contains("file1.txt"));
        assertTrue(dirList.contains("file2.txt"));

        list = virtualDir.list("test-archive/dir1");
        dirList = Arrays.asList(list);
        assertEquals(1, dirList.size());
        assertTrue(dirList.contains("file3.txt"));
    }

    private void assertListInvalidPath() {
        try {
            virtualDir.list("in/valid/path");
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }
}
