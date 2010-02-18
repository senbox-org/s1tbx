package org.esa.beam.dataio.modis;

import junit.framework.TestCase;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class ModisUtilsTest_CreateDateFromString extends TestCase {

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testOk_MoreThanMillisecondsSnipped() throws ParseException {
        final Calendar cal = GregorianCalendar.getInstance();
        final String date = "2004-07-10";
        // pareable time = "21:55:11.123"
        final String time = "21:55:11.123456";

        final Date testDate = ModisUtils.createDateFromStrings(date, time);

        assertNotNull(testDate);
        cal.setTime(testDate);
        assertEquals(2004, cal.get(Calendar.YEAR));
        assertEquals(7 - 1, cal.get(Calendar.MONTH));
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(21, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(55, cal.get(Calendar.MINUTE));
        assertEquals(11, cal.get(Calendar.SECOND));
        // the trailing after Milliseconds numers "456" where snipped
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    public void testOk_exact() throws ParseException {
        final Calendar cal = GregorianCalendar.getInstance();
        final String date = "2005-08-22";
        final String time = "12:22:09.887";

        final Date testDate = ModisUtils.createDateFromStrings(date, time);

        assertNotNull(testDate);
        cal.setTime(testDate);
        assertEquals(2005, cal.get(Calendar.YEAR));
        assertEquals(8 - 1, cal.get(Calendar.MONTH));
        assertEquals(22, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(22, cal.get(Calendar.MINUTE));
        assertEquals(9, cal.get(Calendar.SECOND));
        assertEquals(887, cal.get(Calendar.MILLISECOND));
    }

    public void testOk_WithoutMilliseconds() throws ParseException {
        final Calendar cal = GregorianCalendar.getInstance();
        final String date = "2007-03-25";
        final String time = "15:16:17";

        final Date testDate = ModisUtils.createDateFromStrings(date, time);

        assertNotNull(testDate);
        cal.setTime(testDate);
        assertEquals(2007, cal.get(Calendar.YEAR));
        assertEquals(3 - 1, cal.get(Calendar.MONTH));
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(16, cal.get(Calendar.MINUTE));
        assertEquals(17, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    public void testOk_WithMillisecondsFragment() throws ParseException {
        final Calendar cal = GregorianCalendar.getInstance();
        final String date = "2001-09-13";
        final String time = "07:08:09.4";

        final Date testDate = ModisUtils.createDateFromStrings(date, time);

        assertNotNull(testDate);
        cal.setTime(testDate);
        assertEquals(2001, cal.get(Calendar.YEAR));
        assertEquals(9 - 1, cal.get(Calendar.MONTH));
        assertEquals(13, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(7, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(8, cal.get(Calendar.MINUTE));
        assertEquals(9, cal.get(Calendar.SECOND));
        assertEquals(400, cal.get(Calendar.MILLISECOND));
    }

    public void testOk_WithoutSeconds() throws ParseException {
        final Calendar cal = GregorianCalendar.getInstance();
        final String date = "2008-04-21";
        final String time = "17:18";

        final Date testDate = ModisUtils.createDateFromStrings(date, time);

        assertNotNull(testDate);
        cal.setTime(testDate);
        assertEquals(2008, cal.get(Calendar.YEAR));
        assertEquals(4 - 1, cal.get(Calendar.MONTH));
        assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(17, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(18, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }
}