package org.esa.beam.binning;

/**
 * Represents the period in which pixel data can contribute to a bin.
 *
 * @author Norman Fomferra
 */
public interface DataPeriod {

    enum Membership {
        PREVIOUS_PERIODS(-1),
        CURRENT_PERIOD(0),
        SUBSEQUENT_PERIODS(+1);

        private final int value;

        Membership(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * @return the start time of the binning period in days (Modified Julian Day units, MJD).
     */
    double getStartTime();

    /**
     * @return the duration of the binning period in days.
     */
    int getDuration();

    /**
     * Compute the membership of a given longitude-time pair to this spatial data-period.
     * The result may be one of
     * <ul>
     * <li><code>0</code> - the longitude-time pair belongs to the current period (given by time and time+duration)</li>
     * <li><code>-1</code> - the longitude-time pair belongs to a previous period</li>
     * <li><code>+1</code> - the longitude-time pair belongs to a following period</li>
     * </ul>
     *
     * @param lon  The longitude in range -180 to 180 degrees.
     * @param time The time in days using Modified Julian Day units
     *
     * @return The membership.
     */
    Membership getObservationMembership(double lon, double time);
}
