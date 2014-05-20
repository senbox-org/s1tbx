package org.esa.beam.dataio.avhrr.noaa.pod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ralf Quast
 */
public class PodTypesTest {

    @Test
    public void testSolarZenithAnglesScaleFactor() throws Exception {
        assertEquals(1.0 / 2, PodTypes.getSolarZenithAnglesMetadata().getScalingFactor(), 0.0);
    }

    @Test
    public void testLatScaleFactor() throws Exception {
        assertEquals(1.0 / 128, PodTypes.getLatMetadata().getScalingFactor(), 0.0);
    }

    @Test
    public void testLonScaleFactor() throws Exception {
        assertEquals(1.0 / 128, PodTypes.getLonMetadata().getScalingFactor(), 0.0);
    }

    @Test
    public void testSlopeScaleFactor() throws Exception {
        assertEquals(1.0 / (1 << 30), PodTypes.getSlopeMetadata().getScalingFactor(), 0.0);
    }

    @Test
    public void testInterceptScaleFactor() throws Exception {
        assertEquals(1.0 / (1 << 22), PodTypes.getInterceptMetadata().getScalingFactor(), 0.0);
    }
}
