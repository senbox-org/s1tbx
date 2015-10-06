package org.esa.snap.pixex.calvalus.ma;

import org.esa.snap.core.datamodel.GeoPos;

import java.util.Date;

/**
 * A record comprises a coordinate and an array of attribute values.
 *
 * @author Norman
 */
// todo Copied from Calvalus: Move to BEAM core!?

public interface Record {

    /**
     * @return The location as (lat,lon) point or {@code null} if the location is not available .
     *         The location is usually represented in form of one or more attribute values.
     */
    GeoPos getLocation();

    /**
     * @return The UTC time in milliseconds or {@code null} if the time is not available.
     *         The location is usually represented in form of one or more attribute values.
     */
    Date getTime();

    /**
     * @return The attribute values.
     *         The array will be empty if this record doesn't have any attributes.
     */
    Object[] getAttributeValues();
}
