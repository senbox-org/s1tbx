package org.esa.beam.statistics.tools;

import static org.junit.Assert.*;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.FeatureUtils;
import org.geotools.feature.FeatureCollection;
import org.junit.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

public class StatisticsDatabaseImplTest {

    private StatisticsDatabaseImpl statisticsDatabase;

    @Before
    public void setUp() throws Exception {
        statisticsDatabase = new StatisticsDatabaseImpl();
    }

    @Test
    public void testDatabaseWithRealData() throws IOException {
        //preparation
        final File shapeFile = new File(this.getClass().getResource("20070504_out_cwbody_desh_gk3.shp").getFile());
        final File mappingFile = new File(this.getClass().getResource("20070504_out_cwbody_desh_gk3_band_mapping.txt").getFile());
        final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = FeatureUtils.loadFeatureCollectionFromShapefile(shapeFile);
        final Properties mapping = new Properties();
        mapping.load(new FileReader(mappingFile));
        final FilenameDateExtractorImpl filenameDateExtractor = new FilenameDateExtractorImpl();
        final ProductData.UTC utc = filenameDateExtractor.getDate(shapeFile);

        //execution
        statisticsDatabase.append(utc, featureCollection, mapping);

        //verification
        assertArrayEquals(new int[]{2007}, statisticsDatabase.getYears());
        assertArrayEquals(new String[]{"algal_1", "yellow_subs"}, statisticsDatabase.getParameterNames(2007));
        final DatabaseRecord[] algal_1_records = statisticsDatabase.getData(2007, "algal_1");
        assertEquals(21, algal_1_records.length);
        assertEquals("1", algal_1_records[0].geomId);
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
}
