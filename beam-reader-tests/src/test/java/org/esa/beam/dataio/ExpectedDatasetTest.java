package org.esa.beam.dataio;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExpectedDatasetTest {

    @Test
    public void testGetDecodeQualification() {
        final ExpectedDataset dataset = new ExpectedDataset();

        dataset.setDecodeQualification("intended");
        assertEquals(DecodeQualification.INTENDED, dataset.getDecodeQualification());

        dataset.setDecodeQualification("INTENDED");
        assertEquals(DecodeQualification.INTENDED, dataset.getDecodeQualification());

        dataset.setDecodeQualification("suitable");
        assertEquals(DecodeQualification.SUITABLE, dataset.getDecodeQualification());

        dataset.setDecodeQualification("SUITABLE");
        assertEquals(DecodeQualification.SUITABLE, dataset.getDecodeQualification());

        dataset.setDecodeQualification(null);
        assertEquals(DecodeQualification.UNABLE, dataset.getDecodeQualification());

        dataset.setDecodeQualification("");
        assertEquals(DecodeQualification.UNABLE, dataset.getDecodeQualification());

        dataset.setDecodeQualification("nope");
        assertEquals(DecodeQualification.UNABLE, dataset.getDecodeQualification());
    }
}
