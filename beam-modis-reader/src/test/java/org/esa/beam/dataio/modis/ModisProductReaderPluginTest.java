package org.esa.beam.dataio.modis;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ModisProductReaderPluginTest extends TestCase {

    private ModisProductReaderPlugIn plugIn;
    private File testFile;

    public void testInputTypes() {
        final Class[] inputTypes = plugIn.getInputTypes();
        assertNotNull(inputTypes);
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    public void testCreateReaderInstance() {
        final ProductReader readerInstance = plugIn.createReaderInstance();
        assertNotNull(readerInstance);
        assertTrue(readerInstance instanceof ModisProductReader);
    }

    public void testGetDefaultFileExtension() {
        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();
        assertNotNull(defaultFileExtensions);
        assertEquals(1, defaultFileExtensions.length);
        assertEquals(".hdf", defaultFileExtensions[0]);
    }

    public void testGetDescription() {
        String description = plugIn.getDescription(Locale.getDefault());
        assertNotNull(description);
        assertEquals("MODIS HDF4 Data Products", description);

        description = plugIn.getDescription(null);
        assertNotNull(description);
        assertEquals("MODIS HDF4 Data Products", description);
    }

    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();
        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("MODIS", formatNames[0]);
    }

    public void testGetInputFile_nullInput() {
        final File file = ModisProductReaderPlugIn.getInputFile(null);
        assertNull(file);
    }

    public void testGetInputFile_stringInput() {
        String testFileName = "test.file";
        final File file = ModisProductReaderPlugIn.getInputFile(testFileName);
        assertNotNull(file);
        assertEquals(testFileName, file.getName());
    }

    public void testGetInputFile_fileInput() {
        final File inputFile = new File("I_am_a.file");
        final File file = ModisProductReaderPlugIn.getInputFile(inputFile);
        assertNotNull(file);
        assertEquals(inputFile.getName(), file.getName());
    }

    public void testHasHdfFileExtension() {
        assertFalse(ModisProductReaderPlugIn.hasHdfFileExtension(null));
        assertFalse(ModisProductReaderPlugIn.hasHdfFileExtension(new File("tonio_und.tom")));
        assertTrue(ModisProductReaderPlugIn.hasHdfFileExtension(new File("I_am_but.hdf")));
    }

    public void testGetProductFileFilter() {
        final BeamFileFilter productFileFilter = plugIn.getProductFileFilter();
        assertNotNull(productFileFilter);

        assertEquals("MODIS", productFileFilter.getFormatName());
        assertEquals(".hdf", productFileFilter.getDefaultExtension());
        assertEquals("MODIS HDF4 Data Products (*.hdf)", productFileFilter.getDescription());
    }

    public void testIsValidInputFile_nullFile() {
        assertFalse(ModisProductReaderPlugIn.isValidInputFile(null));
    }

    public void testIsValidInputFile_notExistingFile() {
        assertFalse(ModisProductReaderPlugIn.isValidInputFile(new File("I/don/not/exist.hdf")));
    }

    public void testIsValidInputFile_nonHdfFile() throws IOException {
        testFile = new File("I_do_exist.txt");
        if (!testFile.createNewFile()) {
            fail("unable to create TestFile");
        }

        assertFalse(ModisProductReaderPlugIn.isValidInputFile(testFile));
    }

    public void testIsValidInputFile_hdfFile() throws IOException {
        testFile = new File("I_do_exist.hdf");
        if (!testFile.createNewFile()) {
            fail("unable to create TestFile");
        }

        assertTrue(ModisProductReaderPlugIn.isValidInputFile(testFile));
    }

    @Override
    protected void setUp() {
        plugIn = new ModisProductReaderPlugIn();
    }

    @Override
    protected void tearDown() throws Exception {
        if (testFile != null) {
            if (!testFile.delete()) {
                fail("unable to delete test file");
            }
        }
    }
}
