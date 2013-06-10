package org.esa.beam.timeseries.core.insitu;

import org.esa.beam.framework.datamodel.GeoPos;

import java.util.Date;

/**
 * A record comprises a coordinate and an array of attribute values for each attribute described in the {@link Header}.
 *
 * @author Norman
 */
public interface Record {

    /**
     * @return The location as (lat,lon) point or {@code null} if the location is not available (see {@link Header#hasLocation()}).
     *         The location is usually represented in form of one or more attribute values.
     */
    GeoPos getLocation();

    /**
     * @return The UTC time in milliseconds or {@code null} if the time is not available (see {@link Header#hasTime()}).
     *         The location is usually represented in form of one or more attribute values.
     */
    Date getTime();

    /**
     * @return The attribute values according to {@link Header#getColumnNames()}.
     *         The array will be empty if this record doesn't have any attributes.
     */
    Object[] getAttributeValues();

    String getStationName();
}
