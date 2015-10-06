package org.esa.snap.statistics.tools;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.statistics.StatisticsOp;
import org.esa.snap.statistics.output.UtilTest;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

public class StatisticsDatabaseTest {

    private StatisticsDatabase statisticsDatabase;

    @Before
    public void setUp() throws Exception {
        statisticsDatabase = new StatisticsDatabase("NAME");

    }

    @Test
    public void testDatabaseWithRealData() throws IOException {
        //preparation
        final File shapeFile = new File(this.getClass().getResource("20070504_out_cwbody_desh_gk3.shp").getFile());
        final File mappingFile = new File(this.getClass().getResource("20070504_out_cwbody_desh_gk3_band_mapping.txt").getFile());
        final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = FeatureUtils.loadFeatureCollectionFromShapefile(shapeFile);
        final Properties mapping = new Properties();
        mapping.load(new FileReader(mappingFile));
        final FilenameDateExtractor filenameDateExtractor = new FilenameDateExtractor();
        final ProductData.UTC utc = filenameDateExtractor.getDate(shapeFile);

        //execution
        statisticsDatabase.append(utc, featureCollection, mapping);

        //verification
        final ObservationYear year = new ObservationYear(2007);
        assertArrayEquals(new ObservationYear[]{year}, statisticsDatabase.getYears());
        final ParameterName algal_1 = new ParameterName("algal_1");
        final ParameterName yellow_subs = new ParameterName("yellow_subs");
        assertArrayEquals(new ParameterName[]{algal_1, yellow_subs}, statisticsDatabase.getParameterNames(year));
        final DatabaseRecord[] algal_1_records = statisticsDatabase.getData(year, new ParameterName("algal_1"));
        assertEquals(21, algal_1_records.length);
        assertEquals(new GeometryID("1"), algal_1_records[0].geomId);
        assertEquals("Hever Tidebecken", algal_1_records[0].geomName);
        final Set<Date> dataDates = algal_1_records[0].getDataDates();
        assertEquals(1, dataDates.size());
        final Date date = dataDates.iterator().next();
        assertEquals(9, algal_1_records[0].getStatDataColumns(date).size());
        assertArrayEquals(
                new String[]{
                        "average",
                        "max_error",
                        "maximum",
                        "median",
                        "minimum",
                        "p90_threshold",
                        "p95_threshold",
                        "sigma",
                        "total",
                },
                algal_1_records[0].getStatDataColumns(date).toArray());
        assertEquals("14.929766660899315", algal_1_records[0].getValue(date, "average"));
        assertEquals("0.030201509529724717", algal_1_records[0].getValue(date, "max_error"));
        assertEquals("30.221426010131836", algal_1_records[0].getValue(date, "maximum"));
        assertEquals("16.29853011692874", algal_1_records[0].getValue(date, "median"));
        assertEquals("0.019916480407118797", algal_1_records[0].getValue(date, "minimum"));
        assertEquals("24.301930142305793", algal_1_records[0].getValue(date, "p90_threshold"));
        assertEquals("28.077118833521382", algal_1_records[0].getValue(date, "p95_threshold"));
        assertEquals("8.967569722301686", algal_1_records[0].getValue(date, "sigma"));
        assertEquals("76.0", algal_1_records[0].getValue(date, "total"));
    }

    @Test
    public void testGetGeomName() throws Exception {
        assertEquals("theName", StatisticsDatabase.getGeomName("ColumnName", UtilTest.createFeature("ColumnName", "theName"), "12"));
        assertEquals("12", StatisticsDatabase.getGeomName(null, UtilTest.createFeature("ColumnName", "theName"), "12"));
    }

    @Test
    public void testDatabaseFilledWithDateForDifferentDates() throws Exception {
        final ProductData.UTC utc1 = ProductData.UTC.parse("2002-03-02", "yyyy-MM-dd");
        final ProductData.UTC utc2 = ProductData.UTC.parse("2002-03-03", "yyyy-MM-dd");
        final SimpleFeature feature1 = UtilTest.createFeature("Param1", "Value1");
        final SimpleFeature feature2 = UtilTest.createFeature("Param1", "Value2");
        final Properties mapping = new Properties();
        mapping.setProperty("Param1", StatisticsOp.MAX_ERROR + "_Param1_LongName");

        //execution
        statisticsDatabase.append(utc1, createCollection(feature1), mapping);
        statisticsDatabase.append(utc2, createCollection(feature2), mapping);

        //verification
        final ObservationYear[] years = statisticsDatabase.getYears();
        final ObservationYear year = new ObservationYear(2002);
        assertArrayEquals(new ObservationYear[]{year}, years);
        final ParameterName[] parameterNames = statisticsDatabase.getParameterNames(years[0]);
        assertArrayEquals(new ParameterName[]{new ParameterName("Param1_LongName")}, parameterNames);
        final DatabaseRecord[] databaseRecords = statisticsDatabase.getData(years[0], parameterNames[0]);
        assertEquals(1, databaseRecords.length);
        final Set<Date> dataDates = databaseRecords[0].getDataDates();
        assertEquals(2, dataDates.size());
    }

    private DefaultFeatureCollection createCollection(SimpleFeature feature1) {
        final DefaultFeatureCollection fc1 = new DefaultFeatureCollection("id", feature1.getFeatureType());
        fc1.add(feature1);
        return fc1;
    }
}
