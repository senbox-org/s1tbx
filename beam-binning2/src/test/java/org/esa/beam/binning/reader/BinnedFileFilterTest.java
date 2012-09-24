package org.esa.beam.binning.reader;

import static org.junit.Assert.*;

import org.junit.*;

public class BinnedFileFilterTest {

    private final String startsWith_oc_cci = "ESACCI-OC-";
    private final String mustContain_oc_cci = "L3";

    private final String mustContain_bins = "-bins";

    private final String endsWith = ".nc";
    private final String any = "__any_text__";

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testIsBinnedName_OC_CCI() {
        assertEquals(false, BinnedFileFilter.isBinnedName("DoNotStartExpected" + any + mustContain_oc_cci + any + endsWith));
        assertEquals(false, BinnedFileFilter.isBinnedName(startsWith_oc_cci + any + "doNotContainExpected" + any + endsWith));
        assertEquals(false, BinnedFileFilter.isBinnedName(startsWith_oc_cci + any + mustContain_oc_cci + any + "endsNotWithExpected"));

        assertEquals(true, BinnedFileFilter.isBinnedName(startsWith_oc_cci + any + mustContain_oc_cci + any + endsWith));
    }

    @Test
    public void testIsBinnedName_BEAM_Binning() {
        assertEquals(false, BinnedFileFilter.isBinnedName("StartsWithAnything" + "doNotContain_bins" + "continuesWithAnything" + endsWith));

        assertEquals(true, BinnedFileFilter.isBinnedName("StartsWithAnything" + mustContain_bins + "continuesWithAnything" + endsWith));
    }
}
