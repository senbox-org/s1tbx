package org.esa.beam.dataio.envi;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class EnviProductReaderTest_WithFileIO extends TestCase {

    private File tempDir;

    @Override
    protected void setUp() throws Exception {
        final String ioTempDir = System.getProperty("java.io.tmpdir");
        tempDir = new File(ioTempDir, "tempEnviProductReaderTest");
        if (tempDir.exists()) {
            TestUtils.deleteFileTree(tempDir);
        }
        assertEquals(true, tempDir.mkdir());
    }

    @Override
    protected void tearDown() throws Exception {
        TestUtils.deleteFileTree(tempDir);
    }

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
    
    private void doTest(String hdrFilename, String imgFilename) throws IOException {
        final File hdrFile = new File(tempDir, hdrFilename);
        assertEquals(true, hdrFile.createNewFile());

        final File imgFile = new File(tempDir, imgFilename);
        assertEquals(true, imgFile.createNewFile());

        assertEquals(imgFile, EnviProductReader.getEnviImageFile(hdrFile));
        // cleanup
        imgFile.delete();
        hdrFile.delete();
    }
}