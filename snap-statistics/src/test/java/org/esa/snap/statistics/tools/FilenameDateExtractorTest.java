package org.esa.snap.statistics.tools;

import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Calendar;

import static org.junit.Assert.*;

public class FilenameDateExtractorTest {

    private FilenameDateExtractor filenameDateExtractor;

    @Before
    public void setUp() throws Exception {
        filenameDateExtractor = new FilenameDateExtractor();
    }

    @Test
    public void testIsValidFilename() {
        // execute valid case
        assertEquals(true, filenameDateExtractor.isValidFilename(new File("20020304_blah.shp")));

        // execute invalid cases
        assertEquals(false, filenameDateExtractor.isValidFilename(new File("20020304_blah.InvalidExtension")));
        assertEquals(false, filenameDateExtractor.isValidFilename(new File("TextInFrontOfDate_20020304_blah.shp")));
        assertEquals(false, filenameDateExtractor.isValidFilename(new File("2002_03_04_InvalidDateFormat.shp")));
    }

    @Test
    public void testGetDate() {
        //execution
        ProductData.UTC date = filenameDateExtractor.getDate(new File("20020304_blah.shp"));

        //verification
        assertNotNull(date);
        final int year = 2002;
        final int month = 3;
        final int day = 4;
        assertEquals(year, date.getAsCalendar().get(Calendar.YEAR));
        assertEquals(month, date.getAsCalendar().get(Calendar.MONTH) + 1);  // +1 ... because the first month of a year has the number 0
        assertEquals(day, date.getAsCalendar().get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testGetDate_null_if_invalid_filename() {
        //execution
        ProductData.UTC date = filenameDateExtractor.getDate(new File("invalid_20020304_blah.shp"));

        //verification
        assertNull(date);
    }
}
