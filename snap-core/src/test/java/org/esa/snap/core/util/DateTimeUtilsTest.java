package org.esa.snap.core.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class DateTimeUtilsTest {

    private static TimeZone defaultTZ;

    @BeforeClass
    public static void setUp() {
        defaultTZ = TimeZone.getDefault();
    }

    @AfterClass
    public static void tearDown() {
        TimeZone.setDefault(defaultTZ);
    }

    @Test
    public void testStringToUTC() throws ParseException {
        Date date = DateTimeUtils.stringToUTC("2012-05-30 02:55:00.000000");
        SimpleDateFormat format;

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        assertEquals("2012-05-30 02:55:00.0", format.format(date));

        TimeZone.setDefault(TimeZone.getTimeZone("CET"));
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        assertEquals("2012-05-30 04:55:00.0", format.format(date));
    }

    @Test
    public void testUtcToString() throws ParseException {
        Date date = DateTimeUtils.stringToUTC("2012-05-30 02:55:00.0");

        String utcToString = DateTimeUtils.utcToString(date);

        assertEquals("2012-05-30 02:55:00.0", utcToString);

    }
}