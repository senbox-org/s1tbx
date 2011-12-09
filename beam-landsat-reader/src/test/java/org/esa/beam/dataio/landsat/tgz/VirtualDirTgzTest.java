package org.esa.beam.dataio.landsat.tgz;

import org.esa.beam.util.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class VirtualDirTgzTest {

    private VirtualDirTgz virtualDir;

    @Test
    public void testOpenTgz() throws IOException {
        File testTgz = getTestFile("test-archive.tgz");

        virtualDir = new VirtualDirTgz(testTgz);
        assertEquals(testTgz.getPath(), virtualDir.getBasePath());
    }

    @Test
    public void testOpenTar() throws IOException {
        File testTar = getTestFile("test-archive.tar");

        virtualDir = new VirtualDirTgz(testTar);
        assertEquals(testTar.getPath(), virtualDir.getBasePath());
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
        final File testTgz = getTestFile("test-archive.tar");

        virtualDir = new VirtualDirTgz(testTgz);
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

    @Test
    public void testTar_getInputStream_invalidPath() throws IOException {
        final File testTgz = getTestFile("test-archive.tar");

        virtualDir = new VirtualDirTgz(testTgz);
        try {
            virtualDir.getInputStream("test-archive/invalid_dir/no.file");
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }

    @Test
    public void testCreateTargetDirInTemp_fromSystemPropertyTmpDir() throws IOException {
        final String tempDirName = System.getProperty("java.io.tmpdir");
        assertNotNull(tempDirName);

        File dirInTemp = null;
        try {
            dirInTemp = VirtualDirTgz.createTargetDirInTemp("wurst");
            assertNotNull(dirInTemp);
            assertTrue(dirInTemp.isDirectory());
            assertEquals(new File(tempDirName, "wurst").getAbsolutePath(), dirInTemp.getAbsolutePath());
        } finally {
            if (dirInTemp != null) {
                FileUtils.deleteTree(dirInTemp);
            }
        }
    }

    @Test
    public void testCreateTargetDirInTemp_fromSystemPropertyUserHome() throws IOException {
        final String oldTempDir = System.getProperty("java.io.tmpdir");
        System.clearProperty("java.io.tmpdir");
        final String userHome = System.getProperty("user.home");
        assertNotNull(userHome);

        File dirInTemp = null;
        try {
            dirInTemp = VirtualDirTgz.createTargetDirInTemp("Schneck");
            assertNotNull(dirInTemp);
            assertTrue(dirInTemp.isDirectory());
            assertEquals(new File(userHome, ".beam/temp/Schneck").getAbsolutePath(), dirInTemp.getAbsolutePath());
        } finally {
            System.setProperty("java.io.tmpdir", oldTempDir);
            if (dirInTemp != null) {
                FileUtils.deleteTree(dirInTemp);
            }
        }
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

    @After
    public void tearDown() {
        if (virtualDir != null) {
            virtualDir.close();
        }
    }

    private File getTestFile(String file) {
        File testTgz = new File("./beam-landsat-reader/src/test/resources/org/esa/beam/dataio/landsat/tgz/" + file);
        if (!testTgz.isFile()) {
            testTgz = new File("./src/test/resources/org/esa/beam/dataio/landsat/tgz/" + file);
        }
        assertTrue(testTgz.isFile());
        return testTgz;
    }
}
