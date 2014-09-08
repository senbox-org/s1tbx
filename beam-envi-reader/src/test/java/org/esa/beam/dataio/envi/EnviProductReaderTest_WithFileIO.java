package org.esa.beam.dataio.envi;

import org.esa.beam.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EnviProductReaderTest_WithFileIO {

    private File tempDir;

    @Before
    public void setUp() throws Exception {
        final String ioTempDir = System.getProperty("java.io.tmpdir");
        tempDir = new File(ioTempDir, "tempEnviProductReaderTest");
        if (tempDir.exists()) {
            FileUtils.deleteTree(tempDir);
        }
        assertEquals(true, tempDir.mkdir());
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteTree(tempDir);
    }

    @Test
    public void testCreateEnviImageFile() throws IOException {
        doTest("envifile.hdr", "envifile.img");
        doTest("envifile.img.hdr", "envifile.img");

        fail("Expected, just testing");
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
    
    private void doTest(String hdrFilename, String imgFilename) throws IOException {
        final File hdrFile = new File(tempDir, hdrFilename);
        assertEquals(true, hdrFile.createNewFile());

        final File imgFile = new File(tempDir, imgFilename);
        assertEquals(true, imgFile.createNewFile());

        assertEquals(imgFile.getCanonicalFile(), EnviProductReader.getEnviImageFile(hdrFile).getCanonicalFile());
        // cleanup
        imgFile.delete();
        hdrFile.delete();
    }
}