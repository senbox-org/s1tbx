package org.esa.beam.statistics.tools;

import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

public class StatisticsDatabase {

    //                     year        param       geomID
    private final TreeMap<Integer, Map<String, Map<Integer, DatabaseRecord>>> yearMap;
    private final String nameColumn;

    public StatisticsDatabase(String nameColumn) {
        yearMap = new TreeMap<Integer, Map<String, Map<Integer, DatabaseRecord>>>();
        this.nameColumn = nameColumn;
    }

    public int[] getYears() {
        Set<Integer> years = yearMap.keySet();
        final Integer[] integers = years.toArray(new Integer[years.size()]);
        final int[] ints = new int[integers.length];
        for (int i = 0; i < integers.length; i++) {
            ints[i] = integers[i];
        }
        return ints;
    }

    public String[] getParameterNames(int year) {
        if (!yearMap.containsKey(year)) {
            return new String[0];
        }
        Set<String> parameterNames = yearMap.get(year).keySet();
        return parameterNames.toArray(new String[parameterNames.size()]);
    }

    public DatabaseRecord[] getData(int year, String parameterName) {
        if (!yearMap.containsKey(year)) {
            return new DatabaseRecord[0];
        }
        final Map<String, Map<Integer, DatabaseRecord>> parameterMap = yearMap.get(year);
        if (!parameterMap.containsKey(parameterName)) {
            return new DatabaseRecord[0];
        }
        Collection<DatabaseRecord> databaseRecords = parameterMap.get(parameterName).values();
        return databaseRecords.toArray(new DatabaseRecord[databaseRecords.size()]);
    }

    public void append(ProductData.UTC date, FeatureCollection<SimpleFeatureType, SimpleFeature> shapeCollection, Properties mappingFile) {
        final Calendar utcCalendar = date.getAsCalendar();
        final int year = utcCalendar.get(Calendar.YEAR);
        final Map<String, Map<Integer, DatabaseRecord>> parameterMap;
        if (yearMap.containsKey(year)) {
            parameterMap = yearMap.get(year);
        } else {
            parameterMap = new HashMap<String, Map<Integer, DatabaseRecord>>();
            yearMap.put(year, parameterMap);
        }

        final Map<String, String> invertedMapping = MapInverter.createInvertedTreeMap(mappingFile);
        final StatisticalMappingAnalyser mappingAnalyser = new StatisticalMappingAnalyser(invertedMapping.keySet());
        final String[] geophysicalParameterNames = mappingAnalyser.getGeophysicalParameterNames();
        final String[] statisticalMeasureNames = mappingAnalyser.getStatisticalMeasureNames();
        for (String geophysicalParameterName : geophysicalParameterNames) {
            parameterMap.put(geophysicalParameterName, new TreeMap<Integer, DatabaseRecord>());
        }

        final FeatureIterator<SimpleFeature> features = shapeCollection.features();
        while (features.hasNext()) {
            SimpleFeature simpleFeature = features.next();
            final String geomIdStr = simpleFeature.getID();
            final int geomId = Integer.parseInt(geomIdStr.substring(geomIdStr.lastIndexOf(".") + 1));
            for (String geophysicalParameterName : geophysicalParameterNames) {
                final Map<Integer, DatabaseRecord> geomDatabaseRecordMap = parameterMap.get(geophysicalParameterName);
                final DatabaseRecord geomRecord;
                if (geomDatabaseRecordMap.containsKey(geomId)) {
                    geomRecord = geomDatabaseRecordMap.get(geomId);
                } else {
                    final String geomName = getGeomName(nameColumn, simpleFeature, geomId);
                    geomRecord = new DatabaseRecord(geomId, geomName);
                    geomDatabaseRecordMap.put(geomId, geomRecord);
                }
                final Map<String, String> statData = new TreeMap<String, String>();
                for (String statisticalMeasureName : statisticalMeasureNames) {
                    final String fullName = statisticalMeasureName + "_" + geophysicalParameterName;
                    final String shortName = invertedMapping.get(fullName);
                    final Object value = simpleFeature.getAttribute(shortName);
                    statData.put(statisticalMeasureName, value == null ? "" : value.toString());
                }
                geomRecord.addStatisticalData(utcCalendar.getTime(), statData);
            }
        }
    }

    public static String getGeomName(String nameColumn, SimpleFeature simpleFeature, int geomId) {
        final String geomName;
        final Object simpleFeatureAttribute = simpleFeature.getAttribute(nameColumn);
        if (simpleFeatureAttribute != null) {
            geomName = simpleFeatureAttribute.toString();
        } else {
            geomName = "" + geomId;
        }
        return geomName;
    }
}
