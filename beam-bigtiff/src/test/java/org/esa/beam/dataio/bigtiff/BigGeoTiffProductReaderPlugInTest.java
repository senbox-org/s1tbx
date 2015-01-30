package org.esa.beam.dataio.bigtiff;


import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;
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
        assertEquals("BigGeoTiff", formatNames[0]);
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

        assertEquals("BigGeoTiff/GeoTiff data product.", description);
    }

    @Test
    public void testProductFileFilter() {
        final BeamFileFilter beamFileFilter = plugIn.getProductFileFilter();

        assertNotNull(beamFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(), beamFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], beamFileFilter.getFormatName());
        assertEquals(true, beamFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }
}
