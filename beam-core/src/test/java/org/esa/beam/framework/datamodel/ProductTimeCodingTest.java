package org.esa.beam.framework.datamodel;

import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 15.06.2010
 * Time: 10:27:01
 */
public class ProductTimeCodingTest {

    public static ProductData.UTC UTC_10_06_2010;
    public static ProductData.UTC UTC_15_06_2010;
    public static ProductData.UTC UTC_18_06_2010;

    @BeforeClass
    public static void setUpClass() {
        try {
            UTC_10_06_2010 = ProductData.UTC.parse("10-06-2010", "dd-MM-yyyy");
            UTC_15_06_2010 = ProductData.UTC.parse("15-06-2010", "dd-MM-yyyy");
            UTC_18_06_2010 = ProductData.UTC.parse("18-06-2010", "dd-MM-yyyy");
        } catch (ParseException ignore) {
        }
    }


    @Test
    public void testAddTimeCoding() {
        final Product product = new Product("dummy", "type", 10, 10);
        TimeCoding timeCoding = product.getTimeCoding();
        assertEquals(null, timeCoding);
        product.setTimeCoding(new DefaultTimeCoding(UTC_15_06_2010, UTC_18_06_2010, 10, 10));
        timeCoding = product.getTimeCoding();
        assertNotNull(timeCoding);
        assertEquals(UTC_15_06_2010.getAsDate().getTime(), timeCoding.getStartTime().getAsDate().getTime());
        assertEquals(UTC_18_06_2010.getAsDate().getTime(), timeCoding.getEndTime().getAsDate().getTime());

        timeCoding.setStartTime(UTC_10_06_2010);
        assertEquals(UTC_10_06_2010.getAsDate().getTime(), product.getStartTime().getAsDate().getTime());

        timeCoding.setEndTime(UTC_15_06_2010);
        assertEquals(UTC_15_06_2010.getAsDate().getTime(), product.getEndTime().getAsDate().getTime());

        product.setStartTime(UTC_15_06_2010);
        assertEquals(UTC_15_06_2010.getAsDate().getTime(), timeCoding.getStartTime().getAsDate().getTime());

        product.setEndTime(UTC_18_06_2010);
        assertEquals(UTC_18_06_2010.getAsDate().getTime(), timeCoding.getEndTime().getAsDate().getTime());
    }

    @Test
    public void testSettingNullAsTimeCoding() throws Exception {
        final Product product = new Product("dummy", "type", 10, 10);
        product.setTimeCoding(null);
        assertNull(product.getTimeCoding());

        product.setTimeCoding(new DefaultTimeCoding(UTC_15_06_2010, UTC_18_06_2010, 10, 10));
        assertNotNull(product.getTimeCoding());

        product.setTimeCoding(null);
        assertNull(product.getTimeCoding());
    }
}
