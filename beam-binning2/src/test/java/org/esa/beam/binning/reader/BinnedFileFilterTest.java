package org.esa.beam.binning.reader;

import org.esa.beam.util.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertEquals;

public class BinnedFileFilterTest {

    static final File TESTDATA_DIR = new File("target/binning-test-io");
    private static final String endsWith = ".nc";
    private BinnedFileFilter filter;

    @Before
    public void setUp() throws Exception {
        filter = new BinnedFileFilter();

        if (!TESTDATA_DIR.mkdirs()) {
            fail("Unable to create test data directory");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (TESTDATA_DIR.isDirectory()) {
            if (!FileUtils.deleteTree(TESTDATA_DIR)) {
                fail("Unable to delete test data directory");
            }
        }
    }

    @Test
    public void testConstruction() {
        assertEquals(BinnedProductReaderPlugin.FORMAT_NAME, filter.getFormatName());

        final String[] extensions = filter.getExtensions();
        assertEquals(1, extensions.length);
        assertEquals(BinnedProductReaderPlugin.FILE_EXTENSION, extensions[0]);

        assertEquals(BinnedProductReaderPlugin.FORMAT_DESCRIPTION + " (*.nc)", filter.getDescription());
    }

    @Test
    public void testAccept() {
        assertTrue(filter.accept(TESTDATA_DIR));
        assertTrue(filter.accept(new File("ESACCI-OC-L3S-OC_PRODUCTS-MERGED-1M_MONTHLY_4km_PML_TEST_COMPOSITE_200306.nc")));

        assertFalse(filter.accept(new File("ESACCI-OC-L3S-OC_PRODUCTS-MERGED-1M_MONTHLY_4km_PML_TEST_COMPOSITE_200306")));
        assertFalse(filter.accept(new File("MER_FR__1PNUPA20070708_164713_000000982059_00384_27993_4986.N1")));
        assertFalse(filter.accept(new File("MER_FR__1PNUPA20070708_164713_000000982059_00384_27993_4986.nc")));
    }

    @Test
    public void testIsBinnedName_OC_CCI() {
        final String startsWith_oc_cci = "ESACCI-OC-";
        final String mustContain_oc_cci = "L3";
        final String any = "__any_text__";

        assertEquals(false, BinnedFileFilter.isBinnedName("DoNotStartExpected" + any + mustContain_oc_cci + any + endsWith));
        assertEquals(false, BinnedFileFilter.isBinnedName(startsWith_oc_cci + any + "doNotContainExpected" + any + endsWith));
        assertEquals(false, BinnedFileFilter.isBinnedName(startsWith_oc_cci + any + mustContain_oc_cci + any + "endsNotWithExpected"));

        assertEquals(true, BinnedFileFilter.isBinnedName(startsWith_oc_cci + any + mustContain_oc_cci + any + endsWith));
    }

    @Test
    public void testIsBinnedName_BEAM_Binning() {
        final String mustContain_bins = "-bins";

        assertEquals(false, BinnedFileFilter.isBinnedName("StartsWithAnything" + "doNotContain_bins" + "continuesWithAnything" + endsWith));

        assertEquals(true, BinnedFileFilter.isBinnedName("StartsWithAnything" + mustContain_bins + "continuesWithAnything" + endsWith));
    }
}
