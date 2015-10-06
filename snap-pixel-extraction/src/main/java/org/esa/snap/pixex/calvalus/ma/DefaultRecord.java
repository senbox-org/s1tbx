package org.esa.snap.pixex.calvalus.ma;

import org.esa.snap.core.datamodel.GeoPos;

import java.util.Arrays;
import java.util.Date;

/**
 * A default implementation of a {@link Record}.
 *
 * @author MarcoZ
 * @author Norman
 */

// todo Copied from Calvalus: Move to BEAM core!?

public class DefaultRecord implements Record {
    private final GeoPos location;
    private final Date time;
    private final Object[] values;

    public DefaultRecord(Object... values) {
        this(null, null, values);
    }

    public DefaultRecord(GeoPos location, Date time, Object[] values) {
        this.location = location;
        this.time = time;
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
    public Object[] getAttributeValues() {
        return values;
    }

    @Override
    public String toString() {
        return "DefaultRecord{" +
                "location=" + location +
                ", time=" + time +
                ", values=" + Arrays.asList(values) +
                '}';
    }
}
