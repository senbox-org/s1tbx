package org.esa.snap.statistics.tools;

import org.esa.snap.core.datamodel.ProductData;
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

    private final TreeMap<ObservationYear, Map<ParameterName, Map<GeometryID, DatabaseRecord>>> yearMap;
    private final String nameColumn;

    public StatisticsDatabase(String nameColumn) {
        yearMap = new TreeMap<ObservationYear, Map<ParameterName, Map<GeometryID, DatabaseRecord>>>();
        this.nameColumn = nameColumn;
    }

    public ObservationYear[] getYears() {
        Set<ObservationYear> years = yearMap.keySet();
        return years.toArray(new ObservationYear[years.size()]);
    }

    public ParameterName[] getParameterNames(ObservationYear year) {
        if (!yearMap.containsKey(year)) {
            return new ParameterName[0];
        }
        Set<ParameterName> parameterNames = yearMap.get(year).keySet();
        return parameterNames.toArray(new ParameterName[parameterNames.size()]);
    }

    public static void main(String[] args) {
        final ObservationYear year = new ObservationYear(2002);
        final String string = "No data for year '" + year + "'.";
        System.out.println("string = " + string);
    }

    public DatabaseRecord[] getData(ObservationYear year, ParameterName parameterName) {
        if (!yearMap.containsKey(year)) {
            throw new IllegalArgumentException("No data for year '" + year + "'.");
        }
        final Map<ParameterName, Map<GeometryID, DatabaseRecord>> parameterMap = yearMap.get(year);
        if (!parameterMap.containsKey(parameterName)) {
            throw new IllegalArgumentException("No data for parameter '" + parameterName + "'.");
        }
        Collection<DatabaseRecord> databaseRecords = parameterMap.get(parameterName).values();
        return databaseRecords.toArray(new DatabaseRecord[databaseRecords.size()]);
    }

    public void append(ProductData.UTC date, FeatureCollection<SimpleFeatureType, SimpleFeature> shapeCollection, Properties mapping) {
        final Calendar utcCalendar = date.getAsCalendar();
        final ObservationYear year = new ObservationYear(utcCalendar.get(Calendar.YEAR));
        final Map<ParameterName, Map<GeometryID, DatabaseRecord>> parameterMap = getParameterMap(year);

        final Map<String, String> invertedMapping = MapInverter.createInvertedTreeMap(mapping);
        final StatisticalMappingAnalyser mappingAnalyser = new StatisticalMappingAnalyser(invertedMapping.keySet());
        final String[] geophysicalParameterNames = mappingAnalyser.getGeophysicalParameterNames();
        final String[] statisticalMeasureNames = mappingAnalyser.getStatisticalMeasureNames();
        for (String geophysicalParameterName : geophysicalParameterNames) {
            final ParameterName parameterName = new ParameterName(geophysicalParameterName);
            if (!parameterMap.containsKey(parameterName)) {
                parameterMap.put(parameterName, new TreeMap<GeometryID, DatabaseRecord>());
            }
        }

        final FeatureIterator<SimpleFeature> features = shapeCollection.features();
        while (features.hasNext()) {
            SimpleFeature simpleFeature = features.next();
            final String geomIdStr = simpleFeature.getID();
            final String geomId = geomIdStr.contains(".") ? geomIdStr.substring(geomIdStr.lastIndexOf(".") + 1) : geomIdStr;
            final GeometryID geometryID = new GeometryID(geomId);
            for (String geophysicalParameterName : geophysicalParameterNames) {
                final ParameterName parameterName = new ParameterName(geophysicalParameterName);
                final Map<GeometryID, DatabaseRecord> geomDatabaseRecordMap = parameterMap.get(parameterName);
                final DatabaseRecord geomRecord;
                if (geomDatabaseRecordMap.containsKey(geometryID)) {
                    geomRecord = geomDatabaseRecordMap.get(geometryID);
                } else {
                    final String geomName = getGeomName(nameColumn, simpleFeature, geomId);
                    geomRecord = new DatabaseRecord(geometryID, geomName);
                    geomDatabaseRecordMap.put(geometryID, geomRecord);
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
        features.close();
    }

    private Map<ParameterName, Map<GeometryID, DatabaseRecord>> getParameterMap(ObservationYear year) {
        final Map<ParameterName, Map<GeometryID, DatabaseRecord>> parameterMap;
        if (yearMap.containsKey(year)) {
            parameterMap = yearMap.get(year);
        } else {
            parameterMap = new HashMap<ParameterName, Map<GeometryID, DatabaseRecord>>();
            yearMap.put(year, parameterMap);
        }
        return parameterMap;
    }

    public static String getGeomName(String nameColumn, SimpleFeature simpleFeature, String geomId) {
        final String geomName;
        final Object simpleFeatureAttribute = simpleFeature.getAttribute(nameColumn);
        if (simpleFeatureAttribute != null) {
            geomName = simpleFeatureAttribute.toString();
        } else {
            geomName = geomId;
        }
        return geomName;
    }
}
