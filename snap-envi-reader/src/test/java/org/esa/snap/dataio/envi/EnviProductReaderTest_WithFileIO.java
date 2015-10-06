package org.esa.snap.dataio.envi;

import org.esa.snap.core.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class EnviProductReaderTest_WithFileIO {

    private File tempDir;

    @Before
    public void setUp() throws Exception {
        final String ioTempDir = System.getProperty("java.io.tmpdir");
        tempDir = new File(ioTempDir, "tempEnviProductReaderTest");
        if (tempDir.exists()) {
            assertTrue(FileUtils.deleteTree(tempDir));
        }
        assertTrue(tempDir.mkdir());
    }

    @After
    public void tearDown() throws Exception {
        assertTrue(FileUtils.deleteTree(tempDir));
    }

    @Test
    public void testCreateEnviImageFile() throws IOException {
        doTest("envifile.hdr", "envifile.img");
        doTest("envifile.img.hdr", "envifile.img");

        doTest("envifile.hdr", "envifile.IMG");
        doTest("envifile.HDR", "envifile.img");
        doTest("envifile.HDR", "envifile.IMG");

        doTest("envifile.hdr", "envifile.bin");
        doTest("envifile.img.hdr", "envifile.bin");
        doTest("envifile.bin.hdr", "envifile.bin");

        doTest("envifile.hdr", "envifile.bil");
        doTest("envifile.bil.hdr", "envifile.bil");

        doTest("envifile.hdr", "envifile.bsq");
        doTest("envifile.bsq.hdr", "envifile.bsq");
    }

    @Test
    public void testCreateEnviImageFile_mixedCaseExtension() throws IOException {
        doTest("envifile.hdr", "envifile.iMg");
        doTest("envifile.img.hdr", "envifile.ImG");
    }

    @Test
    public void testCreateEnviImageFile_missingImageFile() throws IOException {
        final File hdrFile = new File(tempDir, "envi.hdr");
        assertTrue(hdrFile.createNewFile());

        try {
            EnviProductReader.getEnviImageFile(hdrFile);
            fail("IOException expected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("No matching ENVI image file found for header file:"));
        }
    }

    private void doTest(String hdrFilename, String imgFilename) throws IOException {
        final File hdrFile = new File(tempDir, hdrFilename);
        assertTrue(hdrFile.createNewFile());

        final File imgFile = new File(tempDir, imgFilename);
        assertTrue(imgFile.createNewFile());

        try {
            assertEquals(imgFile.getCanonicalFile(), EnviProductReader.getEnviImageFile(hdrFile).getCanonicalFile());
        } finally {
            // cleanup
            imgFile.delete();
            hdrFile.delete();
        }
    }
}
