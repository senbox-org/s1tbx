package org.esa.beam.binning.reader;

import static org.junit.Assert.*;

import org.junit.*;

public class BinnedFileFilterTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testIsBinnedName() {
        final String startsWith = "ESACCI-OC-";
        final String mustContain = "L3";
        final String endsWith = ".nc";
        final String any = "__any_text__";

        assertEquals(false, BinnedFileFilter.isBinnedName("DoNotStartExpected" + any + mustContain + any + endsWith));
        assertEquals(false, BinnedFileFilter.isBinnedName(startsWith + any + "doNotContainExpected" + any + endsWith));
        assertEquals(false, BinnedFileFilter.isBinnedName(startsWith + any + mustContain + any + "endsNotWithExpected"));

        assertEquals(true, BinnedFileFilter.isBinnedName(startsWith + any + mustContain + any + endsWith));
    }
}
