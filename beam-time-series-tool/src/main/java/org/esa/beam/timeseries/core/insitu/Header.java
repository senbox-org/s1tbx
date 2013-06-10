package org.esa.beam.timeseries.core.insitu;

/**
 * Interface for accessing meta-information about in-situ data.
 *
 * @author Sabine Embacher
 * @author Thomas Storm
 */
public interface Header {

    /**
     * @return {@code true}, if records that conform to this header return location values (see {@link Record#getLocation()}).
     */
    boolean hasLocation();

    /**
     * @return {@code true}, if records that conform to this header return time values (see {@link Record#getTime()}).
     */
    boolean hasTime();

    /**
     * @return {@code true}, if records that conform to this header return station name values (see {@link Record#getStationName()}).
     */
    boolean hasStationName();

    /**
     * @return The array of parameter names.
     */
    String[] getParameterNames();

    /**
     * @return The array of column names.
     */
    String[] getColumnNames();
}
