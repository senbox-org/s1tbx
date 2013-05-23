package org.esa.beam.binning;

/**
 * Represents the definition of a "spatial data-day", or
 * more generally, a spatial data-period used for binning.
 *
 * @author Norman Fomferra
 */
public interface SpatialDataPeriod {
    /**
     * @return the start time of the binning period in days (Modified Julian Day units, MJD).
     */
    double getStartTime();

    /**
     * @return the duration of the binning period in days.
     */
    int getDuration();

    /**
     * Compute the membership of a given longitude-time pair with respect to the definition a spatial "data-day", or
     * more generally, a spatial data-period.
     * The result may be one of
     * <ul>
     * <li><code>0</code> - the longitude-time pair belongs to the current period (given by time and time+duration)</li>
     * <li><code>-1</code> - the longitude-time pair belongs to a previous period</li>
     * <li><code>+1</code> - the longitude-time pair belongs to a following period</li>
     * </ul>
     *
     * @param lon  The longitude in range -180 to 180 degrees.
     * @param time The time in days using Modified Julian Day units
     * @return The membership.
     */
    int getMembership(double lon, double time);
}
