package org.esa.beam.dataio.modis;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductReader;

import java.io.File;
import java.util.Locale;

public class ModisProductReaderPluginTest extends TestCase {

    private ModisProductReaderPlugIn plugIn;

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
        assertTrue(ModisProductReaderPlugIn.hasHdfFileExtension(new File("I_am_but.hdf"))) ;
    }

    @Override
    protected void setUp() {
        plugIn = new ModisProductReaderPlugIn();
    }
}
