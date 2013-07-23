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

public class LandsatLegacyGeotiffReaderPluginTest {

    @Test
    public void testIsCompressedFile() {
        assertTrue(LandsatLegacyGeotiffReaderPlugin.isCompressedFile(new File("test.zip")));
        assertTrue(LandsatLegacyGeotiffReaderPlugin.isCompressedFile(new File("test.tar")));
        assertTrue(LandsatLegacyGeotiffReaderPlugin.isCompressedFile(new File("test.tgz")));
        assertTrue(LandsatLegacyGeotiffReaderPlugin.isCompressedFile(new File("test.tar.gz")));
        assertTrue(LandsatLegacyGeotiffReaderPlugin.isCompressedFile(new File("test.gz")));

        assertFalse(LandsatLegacyGeotiffReaderPlugin.isCompressedFile(new File("test.txt")));
        assertFalse(LandsatLegacyGeotiffReaderPlugin.isCompressedFile(new File("test.doc")));
        assertFalse(LandsatLegacyGeotiffReaderPlugin.isCompressedFile(new File("test.xml")));
        assertFalse(LandsatLegacyGeotiffReaderPlugin.isCompressedFile(new File("test")));
    }

    @Test
    public void testGetInput_Directory_File() throws IOException {
        final File testDirectory = TestUtil.getTestDirectory("tgz");

        final VirtualDir input = LandsatLegacyGeotiffReaderPlugin.getInput(testDirectory);
        assertNotNull(input);
        assertEquals(testDirectory.getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_Directory_String() throws IOException {
        final File testDirectory = TestUtil.getTestDirectory("tgz");

        final VirtualDir input = LandsatLegacyGeotiffReaderPlugin.getInput(testDirectory.getPath());
        assertNotNull(input);
        assertEquals(testDirectory.getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_File_File() throws IOException {
        final File testFile = TestUtil.getTestFile("geotiff/test_MTL.txt");

        final VirtualDir input = LandsatLegacyGeotiffReaderPlugin.getInput(testFile);
        assertNotNull(input);
        assertEquals(testFile.getAbsoluteFile().getParentFile().getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_File_String() throws IOException {
        final File testFile = TestUtil.getTestFile("geotiff/test_MTL.txt");

        final VirtualDir input = LandsatLegacyGeotiffReaderPlugin.getInput(testFile.getPath());
        assertNotNull(input);
        assertEquals(testFile.getAbsoluteFile().getParentFile().getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_Zip_File() throws IOException {
        final File testFile = TestUtil.getTestFile("zip/test-archive.zip");

        final VirtualDir input = LandsatLegacyGeotiffReaderPlugin.getInput(testFile);
        assertNotNull(input);
        assertEquals(testFile.getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_Tar_File() throws IOException {
        final File testFile = TestUtil.getTestFile("tgz/test-archive.tar");

        final VirtualDir input = LandsatLegacyGeotiffReaderPlugin.getInput(testFile);
        assertNotNull(input);
        assertEquals(testFile.getPath(), input.getBasePath());
    }

    @Test
    public void testGetInput_Tgz_File() throws IOException {
        final File testFile = TestUtil.getTestFile("tgz/test-archive.tgz");

        final VirtualDir input = LandsatLegacyGeotiffReaderPlugin.getInput(testFile);
        assertNotNull(input);
        assertEquals(testFile.getPath(), input.getBasePath());
    }

    @Test
    public void testIsMetadataFile() {
        final File metaFile = new File("L5043033_03319950627_MTL.txt");
        final File nonMetaFile = new File("L5043033_03319950627_B50.TIF");

        assertTrue(LandsatLegacyGeotiffReaderPlugin.isMetadataFile(metaFile));
        assertFalse(LandsatLegacyGeotiffReaderPlugin.isMetadataFile(nonMetaFile));
    }

    @Test
    public void testGetInputTypes() {
        final LandsatLegacyGeotiffReaderPlugin plugin = new LandsatLegacyGeotiffReaderPlugin();

        final Class[] inputTypes = plugin.getInputTypes();
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testIsMatchingArchiveFileName() {
        assertTrue(LandsatLegacyGeotiffReaderPlugin.isMatchingArchiveFileName("L5_30m19950627.tgz"));
        assertTrue(LandsatLegacyGeotiffReaderPlugin.isMatchingArchiveFileName("L5_60m19950627.tgz"));
        assertTrue(LandsatLegacyGeotiffReaderPlugin.isMatchingArchiveFileName("LE71810402006015ASN00.tar"));
        assertTrue(LandsatLegacyGeotiffReaderPlugin.isMatchingArchiveFileName("L7_1810402006015ASN00.tar"));

        assertFalse(LandsatLegacyGeotiffReaderPlugin.isMatchingArchiveFileName("ATS_TOA_1PPTOM20070110_192521_000000822054_00328_25432_0001.N1"));
        assertFalse(LandsatLegacyGeotiffReaderPlugin.isMatchingArchiveFileName("SchnickSchnack.zip"));
        assertFalse(LandsatLegacyGeotiffReaderPlugin.isMatchingArchiveFileName("hoppla.txt"));
        assertFalse(LandsatLegacyGeotiffReaderPlugin.isMatchingArchiveFileName(""));
    }
}
