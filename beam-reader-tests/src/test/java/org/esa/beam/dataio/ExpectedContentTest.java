package org.esa.beam.dataio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExpectedContentTest {

    @Test
    public void testDefaultConstruction() {
        final ExpectedContent expectedContent = new ExpectedContent();

        final ExpectedMetadata[] metadata = expectedContent.getMetadata();
        assertNotNull(metadata);
        assertEquals(0, metadata.length);

        final ExpectedSampleCoding[] flagCodings = expectedContent.getFlagCodings();
        assertNotNull(flagCodings);
        assertEquals(0, flagCodings.length);

        final ExpectedSampleCoding[] indexCodings = expectedContent.getIndexCodings();
        assertNotNull(indexCodings);
        assertEquals(0, indexCodings.length);

        final ExpectedTiePointGrid[] tiePointGrids = expectedContent.getTiePointGrids();
        assertNotNull(tiePointGrids);
        assertEquals(0, tiePointGrids.length);

        final ExpectedBand[] bands = expectedContent.getBands();
        assertNotNull(bands);
        assertEquals(0, bands.length);

        final ExpectedMask[] masks = expectedContent.getMasks();
        assertNotNull(masks);
        assertEquals(0, masks.length);
    }
}
