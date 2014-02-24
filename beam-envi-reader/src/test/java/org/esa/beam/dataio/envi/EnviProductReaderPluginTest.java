package org.esa.beam.dataio.envi;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class EnviProductReaderPluginTest extends TestCase {

    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = readerPlugIn.getDefaultFileExtensions();
        assertEquals(2, defaultFileExtensions.length);
        assertEquals(".hdr", defaultFileExtensions[0]);
        assertEquals(".zip", defaultFileExtensions[1]);
    }

    public void testInputTypes() {
        final Class[] inputTypes = readerPlugIn.getInputTypes();
        assertNotNull(inputTypes);
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    public void testDecodeQualfication_wrongObject() {
        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(new Double(23.8)));
    }

    public void testDecodeQualfication_Stream_success() {
        final String line = EnviConstants.FIRST_LINE + "dklfj\n234j\nsdf\tsdf\tsdaf\n";
        final ByteArrayInputStream bais = new ByteArrayInputStream(line.getBytes());
        final ImageInputStream stReader = new MemoryCacheImageInputStream(bais);
        assertEquals(DecodeQualification.SUITABLE, readerPlugIn.getDecodeQualification(stReader));
    }

    public void testDecodeQualfication_Stream_fail_invalid() {
        final String lines = "This is no success!";
        final ByteArrayInputStream bais = new ByteArrayInputStream(lines.getBytes());
        final ImageInputStream stReader = new MemoryCacheImageInputStream(bais);
        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(stReader));
    }

    public void testDecodeQualification_File_success() throws IOException {
        final String line = EnviConstants.FIRST_LINE + "dklfj\n234j\nsdf\tsdf\tsdaf\n";
        writeToTestFile(line);

        assertEquals(DecodeQualification.SUITABLE, readerPlugIn.getDecodeQualification(headerFile));
    }

    public void testDecodeQualification_File_failure() throws IOException {
        writeToTestFile("blaberrhabarber");

        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(headerFile));
    }

    public void testDecodeQualification_String_success() throws IOException {
        final String line = EnviConstants.FIRST_LINE + "dklfj\n234j\nsdf\tsdf\tsdaf\n";
        writeToTestFile(line);

        assertEquals(DecodeQualification.SUITABLE, readerPlugIn.getDecodeQualification(headerFile.getName()));
    }

    public void testDecodeQualification_String_failure() throws IOException {
        writeToTestFile("blaberrhabarber");

        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(headerFile.getName()));
    }

    public void testDecodeQualification_ZipFile_success() throws IOException {
        final String line = EnviConstants.FIRST_LINE + "dklfj\n234j\nsdf\tsdf\tsdaf\n";
        writeToZipTestFile(line, true);

        assertEquals(DecodeQualification.SUITABLE, readerPlugIn.getDecodeQualification(headerFile));
    }

    public void testDecodeQualification_ZipFile_failure() throws IOException {
        writeToZipTestFile("blaberrhabarber", true);

        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(headerFile));
    }

    public void testDecodeQualification_ZipFile_failureNotEnoughEntries() throws IOException {
        final String line = EnviConstants.FIRST_LINE + "dklfj\n234j\nsdf\tsdf\tsdaf\n";
        writeToZipTestFile(line, false);

        assertEquals(DecodeQualification.UNABLE, readerPlugIn.getDecodeQualification(headerFile));
    }

    public void testCreateReaderInstance() {
        final ProductReader reader = readerPlugIn.createReaderInstance();
        assertNotNull(reader);
        assertSame(EnviProductReader.class, reader.getClass());
    }

    public void testGetInputFile() {
        final String inputFileName = "test.file";

        File file = EnviProductReaderPlugIn.getInputFile(inputFileName);
        assertEquals(inputFileName, file.getName());

        final File inputFile = new File(inputFileName);
        file = EnviProductReaderPlugIn.getInputFile(inputFile);
        assertEquals(inputFileName, file.getName());
    }

    public void testIsCompressedFile() {
        File file = new File("envi.file");
        assertFalse(EnviProductReaderPlugIn.isCompressedFile(file));

        file = new File("envi.zip");
        assertTrue(EnviProductReaderPlugIn.isCompressedFile(file));
    }

    public void testGetFormatNames() {
        final String[] formatNames = readerPlugIn.getFormatNames();
        assertEquals(1, formatNames.length);
        assertEquals("ENVI", formatNames[0]);
    }

    public void testGetDescription() {
        assertEquals("ENVI Data Products", readerPlugIn.getDescription(null));
    }

    public void testGetProductFileFilter() {
        final BeamFileFilter beamFileFilter = readerPlugIn.getProductFileFilter();

        assertEquals(".hdr", beamFileFilter.getDefaultExtension());
        final String[] extensions = beamFileFilter.getExtensions();
        assertEquals(".hdr", extensions[0]);
        assertEquals(".zip", extensions[1]);
        assertEquals("ENVI Data Products (*.hdr,*.zip)", beamFileFilter.getDescription());
        assertEquals("ENVI", beamFileFilter.getFormatName());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private EnviProductReaderPlugIn readerPlugIn;
    private File headerFile;

    protected void setUp() throws Exception {
        readerPlugIn = new EnviProductReaderPlugIn();
    }

    protected void tearDown() throws Exception {
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
