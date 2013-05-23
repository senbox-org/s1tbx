package org.esa.beam.binning.support;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Norman Fomferra
 */
public class SpatialDataPeriodImplTest {
    @Test
    public void testIt() throws Exception {
        final double T0 = 53637.21235;  // any day is fine
        final double H = 1 / 24.;

        final SpatialDataPeriodImpl p = new SpatialDataPeriodImpl(T0, 1, 10.0);

        assertEquals(-1, p.getObservationMembership(-180, T0 - 12 * H));
        assertEquals(-1, p.getObservationMembership(-180, T0 - 6 * H));
        assertEquals(-1, p.getObservationMembership(-180, T0 + 0 * H));
        assertEquals(-1, p.getObservationMembership(-180, T0 + 6 * H));
        assertEquals(0, p.getObservationMembership(-180, T0 + 12 * H));
        assertEquals(0, p.getObservationMembership(-180, T0 + 18 * H));
        assertEquals(0, p.getObservationMembership(-180, T0 + 24 * H));
        assertEquals(0, p.getObservationMembership(-180, T0 + (24 + 6) * H));
        assertEquals(1, p.getObservationMembership(-180, T0 + (24 + 12) * H));

        assertEquals(-1, p.getObservationMembership(-135, T0 - 12 * H));
        assertEquals(-1, p.getObservationMembership(-135, T0 - 6 * H));
        assertEquals(-1, p.getObservationMembership(-135, T0 + 0 * H));
        assertEquals(-1, p.getObservationMembership(-135, T0 + 6 * H));
        assertEquals(0, p.getObservationMembership(-135, T0 + 12 * H));
        assertEquals(0, p.getObservationMembership(-135, T0 + 18 * H));
        assertEquals(0, p.getObservationMembership(-135, T0 + 24 * H));
        assertEquals(0, p.getObservationMembership(-135, T0 + (24 + 6) * H));
        assertEquals(1, p.getObservationMembership(-135, T0 + (24 + 12) * H));


        assertEquals(-1, p.getObservationMembership(-90, T0 - 12 * H));
        assertEquals(-1, p.getObservationMembership(-90, T0 - 6 * H));
        assertEquals(-1, p.getObservationMembership(-90, T0 + 0 * H));
        assertEquals(0, p.getObservationMembership(-90, T0 + 6 * H));
        assertEquals(0, p.getObservationMembership(-90, T0 + 12 * H));
        assertEquals(0, p.getObservationMembership(-90, T0 + 18 * H));
        assertEquals(0, p.getObservationMembership(-90, T0 + 24 * H));
        assertEquals(1, p.getObservationMembership(-90, T0 + (24 + 6) * H));
        assertEquals(1, p.getObservationMembership(-90, T0 + (24 + 12) * H));


        assertEquals(-1, p.getObservationMembership(0, T0 - 12 * H));
        assertEquals(-1, p.getObservationMembership(0, T0 - 6 * H));
        assertEquals(0, p.getObservationMembership(0, T0 + 0 * H));
        assertEquals(0, p.getObservationMembership(0, T0 + 6 * H));
        assertEquals(0, p.getObservationMembership(0, T0 + 12 * H));
        assertEquals(0, p.getObservationMembership(0, T0 + 18 * H));
        assertEquals(1, p.getObservationMembership(0, T0 + 24 * H));
        assertEquals(1, p.getObservationMembership(0, T0 + (24 + 6) * H));
        assertEquals(1, p.getObservationMembership(0, T0 + (24 + 12) * H));


        assertEquals(-1, p.getObservationMembership(90, T0 - 12 * H));
        assertEquals(0, p.getObservationMembership(90, T0 - 6 * H));
        assertEquals(0, p.getObservationMembership(90, T0 + 0 * H));
        assertEquals(0, p.getObservationMembership(90, T0 + 6 * H));
        assertEquals(0, p.getObservationMembership(90, T0 + 12 * H));
        assertEquals(1, p.getObservationMembership(90, T0 + 18 * H));
        assertEquals(1, p.getObservationMembership(90, T0 + 24 * H));
        assertEquals(1, p.getObservationMembership(90, T0 + (24 + 6) * H));
        assertEquals(1, p.getObservationMembership(90, T0 + (24 + 12) * H));


        assertEquals(-1, p.getObservationMembership(135, T0 - 12 * H));
        assertEquals(0, p.getObservationMembership(135, T0 - 6 * H));
        assertEquals(0, p.getObservationMembership(135, T0 + 0 * H));
        assertEquals(0, p.getObservationMembership(135, T0 + 6 * H));
        assertEquals(0, p.getObservationMembership(135, T0 + 12 * H));
        assertEquals(1, p.getObservationMembership(135, T0 + 18 * H));
        assertEquals(1, p.getObservationMembership(135, T0 + 24 * H));
        assertEquals(1, p.getObservationMembership(135, T0 + (24 + 6) * H));
        assertEquals(1, p.getObservationMembership(135, T0 + (24 + 12) * H));


        assertEquals(0, p.getObservationMembership(179, T0 - 12 * H));
        assertEquals(0, p.getObservationMembership(179, T0 - 6 * H));
        assertEquals(0, p.getObservationMembership(179, T0 + 0 * H));
        assertEquals(0, p.getObservationMembership(179, T0 + 6 * H));
        assertEquals(1, p.getObservationMembership(179, T0 + 12 * H));
        assertEquals(1, p.getObservationMembership(179, T0 + 18 * H));
        assertEquals(1, p.getObservationMembership(179, T0 + 24 * H));
        assertEquals(1, p.getObservationMembership(179, T0 + (24 + 6) * H));
        assertEquals(1, p.getObservationMembership(179, T0 + (24 + 12) * H));

    }
}
