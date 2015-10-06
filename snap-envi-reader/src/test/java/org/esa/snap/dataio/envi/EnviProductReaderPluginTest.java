package org.esa.snap.dataio.envi;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;


public class EnviProductReaderPluginTest {

    @Test
    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = readerPlugIn.getDefaultFileExtensions();
        assertEquals(2, defaultFileExtensions.length);
        assertEquals(".hdr", defaultFileExtensions[0]);
        assertEquals(".zip", defaultFileExtensions[1]);
    }

    @Test
    public void testInputTypes() {
        final Class[] inputTypes = readerPlugIn.getInputTypes();
        assertNotNull(inputTypes);
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testDecodeQualfication_wrongObject() {
        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(new Double(23.8)));
    }

    @Test
    public void testDecodeQualfication_Stream_success() {
        final String line = EnviConstants.FIRST_LINE + "dklfj\n234j\nsdf\tsdf\tsdaf\n";
        final ByteArrayInputStream bais = new ByteArrayInputStream(line.getBytes());
        final ImageInputStream stReader = new MemoryCacheImageInputStream(bais);
        assertEquals(DecodeQualification.SUITABLE, readerPlugIn.getDecodeQualification(stReader));
    }

    @Test
    public void testDecodeQualfication_Stream_fail_invalid() {
        final String lines = "This is no success!";
        final ByteArrayInputStream bais = new ByteArrayInputStream(lines.getBytes());
        final ImageInputStream stReader = new MemoryCacheImageInputStream(bais);
        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(stReader));
    }

    @Test
    public void testDecodeQualification_File_success() throws IOException {
        final String line = EnviConstants.FIRST_LINE + "dklfj\n234j\nsdf\tsdf\tsdaf\n";
        writeToTestFile(line);

        assertEquals(DecodeQualification.SUITABLE, readerPlugIn.getDecodeQualification(headerFile));
    }

    @Test
    public void testDecodeQualification_File_failure() throws IOException {
        writeToTestFile("blaberrhabarber");

        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(headerFile));
    }

    @Test
    public void testDecodeQualification_String_success() throws IOException {
        final String line = EnviConstants.FIRST_LINE + "dklfj\n234j\nsdf\tsdf\tsdaf\n";
        writeToTestFile(line);

        assertEquals(DecodeQualification.SUITABLE, readerPlugIn.getDecodeQualification(headerFile.getName()));
    }

    @Test
    public void testDecodeQualification_String_failure() throws IOException {
        writeToTestFile("blaberrhabarber");

        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(headerFile.getName()));
    }

    @Test
    public void testDecodeQualification_ZipFile_success() throws IOException {
        final String line = EnviConstants.FIRST_LINE + "dklfj\n234j\nsdf\tsdf\tsdaf\n";
        writeToZipTestFile(line, true);

        assertEquals(DecodeQualification.SUITABLE, readerPlugIn.getDecodeQualification(headerFile));
    }

    @Test
    public void testDecodeQualification_ZipFile_failure() throws IOException {
        writeToZipTestFile("blaberrhabarber", true);

        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(headerFile));
    }

    @Test
    public void testDecodeQualification_ZipFile_failureNotEnoughEntries() throws IOException {
        final String line = EnviConstants.FIRST_LINE + "dklfj\n234j\nsdf\tsdf\tsdaf\n";
        writeToZipTestFile(line, false);

        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(headerFile));
    }

    @Test
    public void testCreateReaderInstance() {
        final ProductReader reader = readerPlugIn.createReaderInstance();
        assertNotNull(reader);
        assertSame(EnviProductReader.class, reader.getClass());
    }

    @Test
    public void testGetInputFile() {
        final String inputFileName = "test.file";

        File file = EnviProductReaderPlugIn.getInputFile(inputFileName);
        assertEquals(inputFileName, file.getName());

        final File inputFile = new File(inputFileName);
        file = EnviProductReaderPlugIn.getInputFile(inputFile);
        assertEquals(inputFileName, file.getName());
    }

    @Test
    public void testIsCompressedFile() {
        File file = new File("envi.file");
        assertFalse(EnviProductReaderPlugIn.isCompressedFile(file));

        file = new File("envi.zip");
        assertTrue(EnviProductReaderPlugIn.isCompressedFile(file));
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = readerPlugIn.getFormatNames();
        assertEquals(1, formatNames.length);
        assertEquals("ENVI", formatNames[0]);
    }

    @Test
    public void testGetDescription() {
        assertEquals("ENVI Data Products", readerPlugIn.getDescription(null));
    }

    @Test
    public void testGetProductFileFilter() {
        final SnapFileFilter snapFileFilter = readerPlugIn.getProductFileFilter();

        assertEquals(".hdr", snapFileFilter.getDefaultExtension());
        final String[] extensions = snapFileFilter.getExtensions();
        assertEquals(".hdr", extensions[0]);
        assertEquals(".zip", extensions[1]);
        assertEquals("ENVI Data Products (*.hdr,*.zip)", snapFileFilter.getDescription());
        assertEquals("ENVI", snapFileFilter.getFormatName());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private EnviProductReaderPlugIn readerPlugIn;
    private File headerFile;

    @Before
    public void setUp() throws Exception {
        readerPlugIn = new EnviProductReaderPlugIn();
    }

    @After
    public void tearDown() throws Exception {
        if (headerFile != null) {
            if (!headerFile.delete()) {
                fail("unable to delete testdata: " + headerFile.getName());
            }
            headerFile = null;
        }
    }

    private void writeToTestFile(String line) throws IOException {
        headerFile = new File("test.hdr");

        headerFile.createNewFile();
        FileOutputStream outStream = new FileOutputStream(headerFile);
        outStream.write(line.getBytes());
        outStream.flush();
        outStream.close();
    }

    private void writeToZipTestFile(String line, boolean writeImgEntry) throws IOException {
        headerFile = new File("test.zip");
        final ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(headerFile.getName()));
        zipOut.putNextEntry(new ZipEntry("test.hdr"));
        zipOut.write(line.getBytes());
        zipOut.closeEntry();

        if (writeImgEntry) {
            zipOut.putNextEntry(new ZipEntry("test.img"));
            zipOut.write(line.getBytes());
            zipOut.closeEntry();
        }

        zipOut.close();
    }
}
