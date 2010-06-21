package org.esa.beam.framework.datamodel;

import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 16.06.2010
 * Time: 11:13:30
 */
public class DefaultTimeCodingTest {

    private static ProductData.UTC UTC_15_06_2010;

    @BeforeClass
    public static void beforeClass() throws ParseException {
        UTC_15_06_2010 = ProductData.UTC.parse("15-06-2010", "dd-MM-yyyy");

    }

    @Test
    public void testGetTimeWithStartTimeNull() throws Exception {
        final DefaultTimeCoding timeCoding = new DefaultTimeCoding(null, UTC_15_06_2010, 25);
        ProductData.UTC currentTime;

        currentTime = timeCoding.getTime(new PixelPos(10, 24));
        assertNotNull(currentTime);
        assertEquals(UTC_15_06_2010.getAsDate(), currentTime.getAsDate());
        currentTime = timeCoding.getTime(new PixelPos(10, 1));
        assertNotNull(currentTime);
        assertEquals(UTC_15_06_2010.getAsDate(), currentTime.getAsDate());
    }

    @Test
    public void testGetTimeWithEndTimeNull() throws Exception {
        final DefaultTimeCoding timeCoding = new DefaultTimeCoding(UTC_15_06_2010, null, 25);
        ProductData.UTC currentTime;

        currentTime = timeCoding.getTime(new PixelPos(10, 24));
        assertNotNull(currentTime);
        assertEquals(UTC_15_06_2010.getAsDate(), currentTime.getAsDate());
        currentTime = timeCoding.getTime(new PixelPos(10, 1));
        assertNotNull(currentTime);
        assertEquals(UTC_15_06_2010.getAsDate(), currentTime.getAsDate());
    }

    @Test
    public void testGetTimeWithStartAndEndTimeNull() throws Exception {
        final DefaultTimeCoding timeCoding = new DefaultTimeCoding(null, null, 25);
        ProductData.UTC currentTime;

        currentTime = timeCoding.getTime(new PixelPos(10, 24));
        assertNull(currentTime);
    }
}
