package org.esa.beam.dataio.bigtiff;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.util.io.BeamFileFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Locale;

import static org.junit.Assert.*;

public class BigGeoTiffProductWriterPlugInTest {

    private BigGeoTiffProductWriterPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new BigGeoTiffProductWriterPlugIn();
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();
        assertArrayEquals(new String[]{"BigGeoTiff"}, formatNames);
    }

    @Test
    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();
        assertArrayEquals(new String[]{".tif", ".tiff"}, defaultFileExtensions);
    }

    @Test
    public void testGetOutputTypes() {
        final Class[] outputTypes = plugIn.getOutputTypes();
        assertArrayEquals(new Class[]{String.class, File.class,}, outputTypes);
    }

    @Test
    public void testGetDescription() {
        assertEquals("BigGeoTiff/GeoTiff data product.", plugIn.getDescription(null));
    }

    @Test
    public void testProductFileFilter() {
        final BeamFileFilter beamFileFilter = plugIn.getProductFileFilter();

        assertNotNull(beamFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(), beamFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], beamFileFilter.getFormatName());
        assertEquals(true, beamFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }

    @Test
    public void testCreateWriterInstance() {
        final ProductWriter writer = plugIn.createWriterInstance();

        assertNotNull(writer);
        assertTrue(writer instanceof BigGeoTiffProductWriter);
    }
}
