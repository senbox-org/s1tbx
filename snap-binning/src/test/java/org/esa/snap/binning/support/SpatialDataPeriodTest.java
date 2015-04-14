package org.esa.snap.binning.support;

import org.esa.snap.binning.DataPeriod;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * @author Norman Fomferra
 */
public class SpatialDataPeriodTest {

    @Test
    public void testIt() throws Exception {
        final double T0 = 53637.21235;  // any day is fine
        final double H = 1 / 24.;

        final SpatialDataPeriod p = new SpatialDataPeriod(T0, 1, 10.0);

        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-180, T0 - 12 * H));
        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-180, T0 - 6 * H));
        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-180, T0 + 0 * H));
        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-180, T0 + 6 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-180, T0 + 12 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-180, T0 + 18 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-180, T0 + 24 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-180, T0 + (24 + 6) * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(-180, T0 + (24 + 12) * H));

        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-135, T0 - 12 * H));
        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-135, T0 - 6 * H));
        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-135, T0 + 0 * H));
        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-135, T0 + 6 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-135, T0 + 12 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-135, T0 + 18 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-135, T0 + 24 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-135, T0 + (24 + 6) * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(-135, T0 + (24 + 12) * H));


        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-90, T0 - 12 * H));
        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-90, T0 - 6 * H));
        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(-90, T0 + 0 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-90, T0 + 6 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-90, T0 + 12 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-90, T0 + 18 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(-90, T0 + 24 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(-90, T0 + (24 + 6) * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(-90, T0 + (24 + 12) * H));


        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(0, T0 - 12 * H));
        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(0, T0 - 6 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(0, T0 + 0 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(0, T0 + 6 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(0, T0 + 12 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(0, T0 + 18 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(0, T0 + 24 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(0, T0 + (24 + 6) * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(0, T0 + (24 + 12) * H));


        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(90, T0 - 12 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(90, T0 - 6 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(90, T0 + 0 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(90, T0 + 6 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(90, T0 + 12 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(90, T0 + 18 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(90, T0 + 24 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(90, T0 + (24 + 6) * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(90, T0 + (24 + 12) * H));


        assertEquals(DataPeriod.Membership.PREVIOUS_PERIODS, p.getObservationMembership(135, T0 - 12 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(135, T0 - 6 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(135, T0 + 0 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(135, T0 + 6 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(135, T0 + 12 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(135, T0 + 18 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(135, T0 + 24 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(135, T0 + (24 + 6) * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(135, T0 + (24 + 12) * H));


        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(179, T0 - 12 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(179, T0 - 6 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(179, T0 + 0 * H));
        assertEquals(DataPeriod.Membership.CURRENT_PERIOD, p.getObservationMembership(179, T0 + 6 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(179, T0 + 12 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(179, T0 + 18 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(179, T0 + 24 * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(179, T0 + (24 + 6) * H));
        assertEquals(DataPeriod.Membership.SUBSEQUENT_PERIODS, p.getObservationMembership(179, T0 + (24 + 12) * H));

    }
}
