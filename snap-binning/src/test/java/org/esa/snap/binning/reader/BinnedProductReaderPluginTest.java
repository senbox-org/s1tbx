package org.esa.snap.binning.reader;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class BinnedProductReaderPluginTest {

    private BinnedProductReaderPlugin plugin;

    @Before
    public void setUp() {
        plugin = new BinnedProductReaderPlugin();
    }

    @Test
    public void testGetInputTypes() {
        final Class[] inputTypes = plugin.getInputTypes();
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testCreateReaderInstance() {
        final ProductReader readerInstance = plugin.createReaderInstance();
        assertTrue(readerInstance instanceof BinnedProductReader);
    }

    @Test
    public void testGetProductFileFilter() {
        final SnapFileFilter fileFilter = plugin.getProductFileFilter();
        assertTrue(fileFilter instanceof BinnedFileFilter);
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugin.getFormatNames();
        assertEquals(1, formatNames.length);
        assertEquals("Binned_data_product", formatNames[0]);
    }

    @Test
    public void testGetFileExtensions() {
        final String[] fileExtensions = plugin.getDefaultFileExtensions();
        assertEquals(1, fileExtensions.length);
        assertEquals(".nc", fileExtensions[0]);
    }

    @Test
    public void testGetDescription() {
        final String description = plugin.getDescription(null);// don't care about locale tb 2013-07-29
        assertEquals("Reader for SeaDAS-Level-3-alike NetCDF files containing binned Level-3 data", description);
    }

    @Test
    public  void testGetDecodeQualification() {
        assertEquals(DecodeQualification.UNABLE, plugin.getDecodeQualification(null));
        assertEquals(DecodeQualification.UNABLE, plugin.getDecodeQualification("MER_FR__1PNUPA20070708_164713_000000982059_00384_27993_4986.N1"));

        // @todo 3 tb/** add tests for valid file tb 2013-07-29
    }
}
