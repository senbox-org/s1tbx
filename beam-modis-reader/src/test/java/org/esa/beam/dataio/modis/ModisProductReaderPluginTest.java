package org.esa.beam.dataio.modis;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class ModisProductReaderPluginTest {

    private ModisProductReaderPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new ModisProductReaderPlugIn();
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();
        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("MODIS", formatNames[0]);
    }

    @Test
    public void testGetDescription() {
        final String description = plugIn.getDescription(null);
        assertEquals("MODIS HDF4 Data Products", description);
    }

    @Test
    public void testGetDefaultExtensions() {
        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();
        assertNotNull(defaultFileExtensions);
        assertEquals(1, defaultFileExtensions.length);
        assertEquals(".hdf", defaultFileExtensions[0]);
    }

    @Test
    public void testGetProductFileFilter() {
        final BeamFileFilter productFileFilter = plugIn.getProductFileFilter();
        assertNotNull(productFileFilter);

        final String[] extensions = productFileFilter.getExtensions();
        assertNotNull(extensions);
        assertEquals(1, extensions.length);
        assertEquals(".hdf", extensions[0]);

        final String formatName = productFileFilter.getFormatName();
        assertEquals("MODIS", formatName);

        final String description = productFileFilter.getDescription();
        assertEquals("MODIS HDF4 Data Products (*.hdf)", description);
    }

    @Test
    public void testCreateReaderInstance() {
        final ProductReader readerInstance = plugIn.createReaderInstance();
        assertNotNull(readerInstance);

        assertTrue(readerInstance instanceof ModisProductReader);
    }

    @Test
    public void testGetInputTypes() {
        final Class[] inputTypes = plugIn.getInputTypes();
        assertNotNull(inputTypes);
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testGetDecodeQualification_invalidInputObject() {
        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(new Double(34));
        assertEquals(DecodeQualification.UNABLE, decodeQualification);
    }

    @Test
    public void testGetDecodeQualification_nullObject() {
        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(null);
        assertEquals(DecodeQualification.UNABLE, decodeQualification);
    }

    @Test
    public void testGetDecodeQualification_notExistingFile() {
        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(new File("does/not/exist/here/no.hdf"));
        assertEquals(DecodeQualification.UNABLE, decodeQualification);
    }

    @Test
    public void testGetDecodeQualification_fileWithWrongExtension() throws UnsupportedEncodingException {
        final URL url = getClass().getResource("/org/esa/beam/resources/modisdb/MOD02HKM.dd");
        Assert.assertNotNull(url);
        Assert.assertEquals("file", url.getProtocol());

        final String path = URLDecoder.decode(url.getPath(), "UTF-8");
        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(path);
        assertEquals(DecodeQualification.UNABLE, decodeQualification);
    }
}
