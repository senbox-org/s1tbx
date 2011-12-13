package org.esa.beam.dataio.landsat.geotiff;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.dataio.landsat.TestUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LandsatGeotiffReaderPluginTest {

    @Test
    public void testIsCompressedFile() {
        assertTrue(LandsatGeotiffReaderPlugin.isCompressedFile(new File("test.zip")));
        assertTrue(LandsatGeotiffReaderPlugin.isCompressedFile(new File("test.tar")));
        assertTrue(LandsatGeotiffReaderPlugin.isCompressedFile(new File("test.tgz")));
        assertTrue(LandsatGeotiffReaderPlugin.isCompressedFile(new File("test.tar.gz")));
        assertTrue(LandsatGeotiffReaderPlugin.isCompressedFile(new File("test.gz")));

        assertFalse(LandsatGeotiffReaderPlugin.isCompressedFile(new File("test.txt")));
        assertFalse(LandsatGeotiffReaderPlugin.isCompressedFile(new File("test.doc")));
        assertFalse(LandsatGeotiffReaderPlugin.isCompressedFile(new File("test.xml")));
        assertFalse(LandsatGeotiffReaderPlugin.isCompressedFile(new File("test")));
    }

    @Test
    public void testGetInput_Directory_File() throws IOException {
        final File testDirectory = TestUtil.getTestDirectory("tgz");

        final VirtualDir input = LandsatGeotiffReaderPlugin.getInput(testDirectory);
        assertNotNull(input);
        assertEquals(testDirectory.getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_Directory_String() throws IOException {
        final File testDirectory = TestUtil.getTestDirectory("tgz");

        final VirtualDir input = LandsatGeotiffReaderPlugin.getInput(testDirectory.getPath());
        assertNotNull(input);
        assertEquals(testDirectory.getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_File_File() throws IOException {
        final File testFile = TestUtil.getTestFile("geotiff/test_MTL.txt");

        final VirtualDir input = LandsatGeotiffReaderPlugin.getInput(testFile);
        assertNotNull(input);
        assertEquals(testFile.getParentFile().getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_File_String() throws IOException {
        final File testFile = TestUtil.getTestFile("geotiff/test_MTL.txt");

        final VirtualDir input = LandsatGeotiffReaderPlugin.getInput(testFile.getPath());
        assertNotNull(input);
        assertEquals(testFile.getParentFile().getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_Zip_File() throws IOException {
        final File testFile = TestUtil.getTestFile("zip/test-archive.zip");

        final VirtualDir input = LandsatGeotiffReaderPlugin.getInput(testFile);
        assertNotNull(input);
        assertEquals(testFile.getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_Tar_File() throws IOException {
        final File testFile = TestUtil.getTestFile("tgz/test-archive.tar");

        final VirtualDir input = LandsatGeotiffReaderPlugin.getInput(testFile);
        assertNotNull(input);
        assertEquals(testFile.getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_Tgz_File() throws IOException {
        final File testFile = TestUtil.getTestFile("tgz/test-archive.tgz");

        final VirtualDir input = LandsatGeotiffReaderPlugin.getInput(testFile);
        assertNotNull(input);
        assertEquals(testFile.getPath(), input.getBasePath());
    }

    @Test
    public void testIsMetadataFile() {
        final File metaFile = new File("L5043033_03319950627_MTL.txt");
        final File nonMetaFile = new File("L5043033_03319950627_B50.TIF");

        assertTrue(LandsatGeotiffReaderPlugin.isMetadataFile(metaFile));
        assertFalse(LandsatGeotiffReaderPlugin.isMetadataFile(nonMetaFile));
    }
}
