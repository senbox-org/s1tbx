package org.esa.beam.dataio;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExpectedTiePointGridTest {

    private ExpectedTiePointGrid tiePointGrid;

    @Before
    public void setUp() {
        tiePointGrid = new ExpectedTiePointGrid();
    }

    @Test
    public void testIsDescriptionSet() {
        assertFalse(tiePointGrid.isDescriptionSet());

        tiePointGrid.setDescription("schnacks");
        assertTrue(tiePointGrid.isDescriptionSet());
    }

    @Test
    public void testIsOffsetXSet() {
        assertFalse(tiePointGrid.isOffsetXSet());

        tiePointGrid.setOffsetX("3.8");
        assertTrue(tiePointGrid.isOffsetXSet());
    }

    @Test
    public void testIsOffsetYSet() {
        assertFalse(tiePointGrid.isOffsetYSet());

        tiePointGrid.setOffsetY("2.7");
        assertTrue(tiePointGrid.isOffsetYSet());
    }

    @Test
    public void testIsSubSamplingXSet() {
        assertFalse(tiePointGrid.isSubSamplingXSet());

        tiePointGrid.setSubSamplingX("9.3");
        assertTrue(tiePointGrid.isSubSamplingXSet());
    }

    @Test
    public void testIsSubSamplingYSet() {
        assertFalse(tiePointGrid.isSubSamplingYSet());

        tiePointGrid.setSubSamplingY("6.5");
        assertTrue(tiePointGrid.isSubSamplingYSet());
    }

    @Test
    public void testDefaultConstruction() {
        final ExpectedPixel[] expectedPixels = tiePointGrid.getExpectedPixels();
        assertNotNull(expectedPixels);
        assertEquals(0, expectedPixels.length);
    }
}
