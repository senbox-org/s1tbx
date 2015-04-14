package org.esa.snap.statistics.tools;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DatabaseRecord {

    public final GeometryID geomId;
    public final String geomName;
    private final Map<Date, Map<String, String>> data;

    public DatabaseRecord(GeometryID geomId, String geomName) {
        this.geomId = geomId;
        this.geomName = geomName;
        this.data = new TreeMap<Date, Map<String, String>>();
    }

    public void addStatisticalData(Date date, Map<String, String> statData) {
        if (data.containsKey(date)) {
            throw new IllegalStateException("The record already contains data for the day '" + date + "'");
        }
        data.put(date, statData);
    }

    public Set<Date> getDataDates() {
        return data.keySet();
    }

    public Set<String> getStatDataColumns(Date date) {
        final Map<String, String> statData = data.get(date);
        if (statData != null) {
            return statData.keySet();
        } else {
            return null;
        }
    }

    public String getValue(Date date, String statName) {
        if (!data.containsKey(date)) {
            return "";
        }
        final Map<String, String> statData = data.get(date);
        if (!statData.containsKey(statName)) {
            return "";
        }
        return statData.get(statName);
    }
}
