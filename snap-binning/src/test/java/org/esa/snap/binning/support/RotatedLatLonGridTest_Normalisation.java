package org.esa.snap.binning.support;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class RotatedLatLonGridTest_Normalisation {

    private RotatedLatLonGrid rotatedLatLonGrid;

    @Before
    public void setUp() throws Exception {
        rotatedLatLonGrid = new RotatedLatLonGrid(180, 10, 20);
    }

    @Test
    public void testNormalizeLat() {
        assertThat(rotatedLatLonGrid.normalizeLat(90), equalTo(90d));
        assertThat(rotatedLatLonGrid.normalizeLat(0), equalTo(0d));
        assertThat(rotatedLatLonGrid.normalizeLat(-90), equalTo(-90d));
        assertThat(rotatedLatLonGrid.normalizeLat(91), equalTo(-89d));
        assertThat(rotatedLatLonGrid.normalizeLat(-91), equalTo(89d));
    }

    @Test
    public void testNormalizeLon() {
        assertThat(rotatedLatLonGrid.normalizeLon(180), equalTo(180d));
        assertThat(rotatedLatLonGrid.normalizeLon(0), equalTo(0d));
        assertThat(rotatedLatLonGrid.normalizeLon(-180), equalTo(-180d));
        assertThat(rotatedLatLonGrid.normalizeLon(181), equalTo(-179d));
        assertThat(rotatedLatLonGrid.normalizeLon(-181), equalTo(179d));
    }
}
