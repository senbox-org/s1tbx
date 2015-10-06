package org.esa.snap.dataio.bigtiff;


import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;

import static org.junit.Assert.*;

public class BigGeoTiffProductReaderPlugInTest {

    private BigGeoTiffProductReaderPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new BigGeoTiffProductReaderPlugIn();
    }

    @Test
    public void testGetInputTypes() {
        final Class[] inputTypes =  plugIn.getInputTypes();

        assertNotNull(inputTypes);
        assertEquals(3, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
        assertEquals(InputStream.class, inputTypes[2]);
    }

    @Test
    public void testCreateReaderInstance() {
        final ProductReader reader = plugIn.createReaderInstance();
        assertNotNull(reader);
        assertTrue(reader instanceof BigGeoTiffProductReader);
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();

        assertEquals(1, formatNames.length);
        assertEquals(BigGeoTiffProductReaderPlugIn.FORMAT_NAME, formatNames[0]);
    }

    @Test
    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();

        assertEquals(2, defaultFileExtensions.length);
        assertEquals(".tif", defaultFileExtensions[0]);
        assertEquals(".tiff", defaultFileExtensions[1]);
    }

    @Test
    public void testGetDescription() {
        final String description = plugIn.getDescription(null);

        assertNotNull(description);
    }

    @Test
    public void testProductFileFilter() {
        final SnapFileFilter snapFileFilter = plugIn.getProductFileFilter();

        assertNotNull(snapFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(), snapFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], snapFileFilter.getFormatName());
        assertEquals(true, snapFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }
}
