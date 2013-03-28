package org.esa.beam.timeseries.core.insitu.csv;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.timeseries.core.insitu.Record;

import java.util.Arrays;
import java.util.Date;

/**
 * A default implementation of a {@link Record}.
 *
 * @author MarcoZ
 * @author Norman
 */
class DefaultRecord implements Record {

    private final GeoPos location;
    private final Date time;
    private final String stationName;
    private final Object[] values;

    DefaultRecord(GeoPos location, Date time, String stationName, Object[] values) {
        this.location = location;
        this.time = time;
        this.stationName = stationName;
        this.values = values;
    }

    @Override
    public GeoPos getLocation() {
        return location;
    }

    @Override
    public Date getTime() {
        return time;
    }

    @Override
    public String getStationName() {
        return stationName;
    }

    @Override
    public Object[] getAttributeValues() {
        return values;
    }

    @Override
    public String toString() {
        return "DefaultRecord{" +
               "  location=" + location +
               "  , time=" + time +
               "  , stationName=" + stationName +
               "  , values=" + Arrays.asList(values) +
               '}';
    }
}
