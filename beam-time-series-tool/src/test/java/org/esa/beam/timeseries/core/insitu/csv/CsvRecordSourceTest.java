package org.esa.beam.timeseries.core.insitu.csv;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.timeseries.core.insitu.Header;
import org.esa.beam.timeseries.core.insitu.Record;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;

import static org.junit.Assert.*;

public class CsvRecordSourceTest {
    @Test
    public void testSimpleCsv() throws Exception {
        final String CSV = ""
                + "# Test CSV\n"
                + "ID\tLAT\tLONG\tTIME\tName\tCHL\tFLAG\n"
                + "16\t53.1\t13.6\t03.04.2003\tName 1\t0.5\t1\n"
                + "17\t53.3\t13.4\t08.04.2003\tName 2\t0.9\t0\n"
                + "18\t53.1\t13.5\t11.04.2003\tName 3\t0.4\t1\n";

        DateFormat dateFormat = ProductData.UTC.createDateFormat("dd.MM.yyyy");

        CsvRecordSource recordSource = new CsvRecordSource(new StringReader(CSV), dateFormat);
        Header header = recordSource.getHeader();
        assertNotNull(header);
        assertNotNull(header.getColumnNames());
        assertArrayEquals(new String[]{"ID", "LAT", "LONG", "TIME", "Name", "CHL", "FLAG"},
                          header.getColumnNames());
        assertEquals(true, header.hasLocation());
        assertEquals(true, header.hasTime());

        Iterable<Record> records = recordSource.getRecords();
        assertNotNull(records);

        Iterator<Record> iterator = records.iterator();
        assertNotNull(iterator);

        assertTrue(iterator.hasNext());
        Record rec1 = iterator.next();
        assertNotNull(rec1);
        assertArrayEquals(new Object[]{(double) 16, 53.1, 13.6, dateFormat.parse("03.04.2003"), "Name 1", 0.5, (double) 1},
                          rec1.getAttributeValues());
        assertEquals(new GeoPos(53.1F, 13.6F), rec1.getLocation());
        assertEquals(dateFormat.parse("03.04.2003"), rec1.getTime());
        assertEquals("Name 1", rec1.getStationName());

        assertTrue(iterator.hasNext());
        Record rec2 = iterator.next();
        assertNotNull(rec2);
        assertArrayEquals(new Object[]{(double) 17, 53.3, 13.4, dateFormat.parse("08.04.2003"), "Name 2", 0.9, (double) 0},
                          rec2.getAttributeValues());
        assertEquals(new GeoPos(53.3F, 13.4F), rec2.getLocation());
        assertEquals(dateFormat.parse("08.04.2003"), rec2.getTime());
        assertEquals("Name 2", rec2.getStationName());

        assertTrue(iterator.hasNext());
        Record rec3 = iterator.next();
        assertNotNull(rec3);
        assertArrayEquals(new Object[]{(double) 18, 53.1, 13.5, dateFormat.parse("11.04.2003"), "Name 3", 0.4, (double) 1},
                          rec3.getAttributeValues());
        assertEquals(new GeoPos(53.1F, 13.5F), rec3.getLocation());
        assertEquals(dateFormat.parse("11.04.2003"), rec3.getTime());
        assertEquals("Name 3", rec3.getStationName());
    }

    @Test
    public void testSimpleCsvWithMissingValues() throws Exception {
        final String CSV = ""
                + "\n"
                + "# Test CSV\n"
                + "\n"
                + "ID\tLAT\tLONG\tTIME\tName\tCHL\tFLAG\n"
                + "16\t53.1\t13.6\t03.04.2003\t\t0.5\t1\n"
                + "17\t53.3\t13.4\t08.04.2003\t\t\t\n"
                + "18\t53.1\t13.5\t11.04.2003\tA\t0.4\t\n";


        DateFormat dateFormat = ProductData.UTC.createDateFormat("dd.MM.yyyy");

        CsvRecordSource recordSource = new CsvRecordSource(new StringReader(CSV), dateFormat);
        Header header = recordSource.getHeader();
        assertNotNull(header);
        assertNotNull(header.getColumnNames());
        assertArrayEquals(new String[]{"ID", "LAT", "LONG", "TIME", "Name", "CHL", "FLAG"},
                          header.getColumnNames());
        assertEquals(true, header.hasLocation());
        assertEquals(true, header.hasTime());

        Iterable<Record> records = recordSource.getRecords();
        assertNotNull(records);

        Iterator<Record> iterator = records.iterator();
        assertNotNull(iterator);

        assertTrue(iterator.hasNext());
        Record rec1 = iterator.next();
        assertNotNull(rec1);
        assertArrayEquals(new Object[]{(double) 16, 53.1, 13.6, dateFormat.parse("03.04.2003"), null, 0.5, (double) 1},
                          rec1.getAttributeValues());
        assertEquals(new GeoPos(53.1F, 13.6F), rec1.getLocation());
        assertEquals(dateFormat.parse("03.04.2003"), rec1.getTime());
        assertEquals(null, rec1.getStationName());

        assertTrue(iterator.hasNext());
        Record rec2 = iterator.next();
        assertNotNull(rec2);
        assertArrayEquals(new Object[]{(double) 17, 53.3, 13.4, dateFormat.parse("08.04.2003"), null, null, null},
                          rec2.getAttributeValues());
        assertEquals(new GeoPos(53.3F, 13.4F), rec2.getLocation());
        assertEquals(dateFormat.parse("08.04.2003"), rec2.getTime());
        assertEquals(null, rec2.getStationName());

        assertTrue(iterator.hasNext());
        Record rec3 = iterator.next();
        assertNotNull(rec3);
        assertArrayEquals(new Object[]{(double) 18, 53.1, 13.5, dateFormat.parse("11.04.2003"), "A", 0.4, null},
                          rec3.getAttributeValues());
        assertEquals(new GeoPos(53.1F, 13.5F), rec3.getLocation());
        assertEquals(dateFormat.parse("11.04.2003"), rec3.getTime());
        assertEquals("A", rec3.getStationName());
    }

    @Test
    public void testRealLifeCsv() throws Exception {
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("cc-matchup-test-insitu.csv"));
        CsvRecordSource recordSource = new CsvRecordSource(reader, ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss"));

        Header header = recordSource.getHeader();
        assertNotNull(header);
        String[] headerAttributeNames = header.getColumnNames();
        assertNotNull(headerAttributeNames);
        assertEquals(8, headerAttributeNames.length);
        assertArrayEquals(new String[]{"ID", "SITE", "FILE_ID", "LAT", "LONG", "TIME", "CONC_CHL", "KD_490"}, headerAttributeNames);
        assertEquals(true, header.hasLocation());
        assertEquals(true, header.hasTime());

        long t0 = System.currentTimeMillis();
        Iterable<Record> records = recordSource.getRecords();
        int n = 0;
        for (Record record : records) {
            Object[] dataAttributeValues = record.getAttributeValues();
            assertEquals(8, dataAttributeValues.length);
            assertType(n, Double.class, dataAttributeValues[0]);
            assertType(n, Double.class, dataAttributeValues[1]);
            assertType(n, Double.class, dataAttributeValues[2]);
            assertType(n, Double.class, dataAttributeValues[3]);
            assertType(n, Double.class, dataAttributeValues[4]);
            assertType(n, Date.class, dataAttributeValues[5]);
            assertType(n, Double.class, dataAttributeValues[6]);
            assertType(n, Double.class, dataAttributeValues[7]);
            n++;
        }
        assertEquals(11876, n);
        System.out.println("CsvRecordSource read " + n + " records, took " + (System.currentTimeMillis() - t0) + " ms");
    }

    private static void assertType(int n, Class<?> expectedType, Object attributeValue) {
        if (attributeValue != null) {
            assertEquals(String.format("Record #%d: value=%s ", (n + 1), attributeValue), expectedType, attributeValue.getClass());
        }
    }
}
