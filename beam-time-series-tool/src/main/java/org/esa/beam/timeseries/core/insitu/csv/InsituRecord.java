package org.esa.beam.timeseries.core.insitu.csv;

import org.esa.beam.framework.datamodel.GeoPos;

import java.util.Date;

/**
 * An insitu record represents a single item of insitu data, given by position, time, station name, and
 * measurement value.
 *
 * @author Sabine Embacher
 * @author Thomas Storm
 */
public class InsituRecord {

    public final GeoPos pos;
    public final Date time;
    public final String stationName;
    public final double value;

    public InsituRecord(GeoPos pos, Date time, String stationName, double value) {
        this.pos = pos;
        this.time = time;
        this.stationName = stationName;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InsituRecord that = (InsituRecord) o;

        if (Double.compare(that.value, value) != 0) {
            return false;
        }
        if (pos != null ? !pos.equals(that.pos) : that.pos != null) {
            return false;
        }
        if (stationName != null ? !stationName.equals(that.stationName) : that.stationName != null) {
            return false;
        }
        if (time != null ? !time.equals(that.time) : that.time != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = pos != null ? pos.hashCode() : 0;
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (stationName != null ? stationName.hashCode() : 0);
        long temp = value != +0.0d ? Double.doubleToLongBits(value) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
