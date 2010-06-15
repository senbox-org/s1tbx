package org.esa.beam.framework.datamodel;

import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 15.06.2010
 * Time: 10:05:46
 */
public class TimeCodingTest {

    public static ProductData.UTC UTC_15_06_2010;
    public static ProductData.UTC UTC_10_06_2010;

    @BeforeClass
    public static void setUpClass() {
        try {
            UTC_15_06_2010 = ProductData.UTC.parse("15-06-2010", "dd-MM-yyyy");
            UTC_10_06_2010 = ProductData.UTC.parse("10-06-2010", "dd-MM-yyyy");
        } catch (ParseException ignore) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFail1() throws ParseException {
        new MyTimeCoding(null, UTC_15_06_2010);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFail2() throws ParseException {
        new MyTimeCoding(UTC_15_06_2010, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTimesinWrongOrder() throws ParseException {
        new MyTimeCoding(UTC_15_06_2010,
                         UTC_10_06_2010);
    }

    @Test
    public void testSetStartTime() throws Exception {
        final TimeCoding timeCoding = new MyTimeCoding(UTC_10_06_2010,
                                                       UTC_15_06_2010);
        assertEquals(UTC_10_06_2010.getAsDate().getTime(),
                     timeCoding.getStartTime().getAsDate().getTime());
        assertEquals(UTC_15_06_2010.getAsDate().getTime(),
                     timeCoding.getEndTime().getAsDate().getTime());

        final TimeCoding secondTimeCoding = new MyTimeCoding(UTC_10_06_2010,
                                                             UTC_15_06_2010);
        assertTrue(timeCoding.equals(secondTimeCoding));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStartTimeAfterEndTime() throws Exception {
        final TimeCoding timeCoding = new MyTimeCoding(UTC_10_06_2010,
                                                       UTC_15_06_2010);
        timeCoding.setStartTime(ProductData.UTC.parse("16-06-2010", "dd-MM-yyyy"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEndTimeBeforeStartTime() throws Exception {
        final TimeCoding timeCoding = new MyTimeCoding(UTC_10_06_2010,
                                                       UTC_15_06_2010);
        timeCoding.setEndTime(ProductData.UTC.parse("04-06-2010", "dd-MM-yyyy"));
    }

    private static class MyTimeCoding extends TimeCoding {

        private MyTimeCoding(ProductData.UTC startTime, ProductData.UTC endTime) {
            super(startTime, endTime);
        }

        @Override
        public ProductData.UTC getTimeAtPixel(PixelPos pos) {
            return null;
        }
    }
}
