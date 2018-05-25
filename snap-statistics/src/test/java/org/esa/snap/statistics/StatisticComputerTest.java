package org.esa.snap.statistics;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Logger;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.statistics.tools.TimeInterval;
import org.junit.Test;

import static org.junit.Assert.*;

public class StatisticComputerTest {

    @Test
    public void testGetIntervalIndex() {
        ProductData.UTC time_0 = createTime(0);
        ProductData.UTC time_1 = createTime(1);
        ProductData.UTC time_2 = createTime(2);
        ProductData.UTC time_3 = createTime(3);
        ProductData.UTC time_4 = createTime(4);
        ProductData.UTC time_5 = createTime(5);
        ProductData.UTC time_6 = createTime(6);
        TimeInterval[] timeIntervals = new TimeInterval[]{
                new TimeInterval(0, time_1, time_2), new TimeInterval(0, time_2, time_3),
                new TimeInterval(0, time_3, time_4), new TimeInterval(0, time_4, time_5)};
        StatisticComputer statisticsComputer =
                new StatisticComputer(null, null, 0, timeIntervals, Logger.getLogger("StatisticsComputerTest"));

        assertEquals(-1, statisticsComputer.getIntervalIndex(getProduct(time_0, time_1)));
        assertEquals(-1, statisticsComputer.getIntervalIndex(getProduct(time_1)));
        assertEquals(0, statisticsComputer.getIntervalIndex(getProduct(time_1, time_2)));
        assertEquals(0, statisticsComputer.getIntervalIndex(getProduct(time_2)));
        assertEquals(1, statisticsComputer.getIntervalIndex(getProduct(time_2, time_3)));
        assertEquals(1, statisticsComputer.getIntervalIndex(getProduct(time_3)));
        assertEquals(2, statisticsComputer.getIntervalIndex(getProduct(time_3, time_4)));
        assertEquals(2, statisticsComputer.getIntervalIndex(getProduct(time_4)));
        assertEquals(3, statisticsComputer.getIntervalIndex(getProduct(time_4, time_5)));
        assertEquals(-1, statisticsComputer.getIntervalIndex(getProduct(time_5)));
        assertEquals(-1, statisticsComputer.getIntervalIndex(getProduct(time_5, time_6)));


    }

    private ProductData.UTC createTime(int month) {
        return ProductData.UTC.create(new GregorianCalendar(2004, month, 1).getTime(), 0);
    }

    private ProductData.UTC getBefore(ProductData.UTC time) {
        Calendar calendar = time.getAsCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, -6);
        return ProductData.UTC.create(calendar.getTime(), 0);
    }

    private ProductData.UTC getAfter(ProductData.UTC time) {
        Calendar calendar = time.getAsCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, 6);
        return ProductData.UTC.create(calendar.getTime(), 0);
    }

    private Product getProduct(ProductData.UTC around) {
        Product product = new Product("dummy", "dummy");
        product.setStartTime(getBefore(around));
        product.setEndTime(getAfter(around));
        return product;
    }

    private Product getProduct(ProductData.UTC after, ProductData.UTC before) {
        Product product = new Product("dummy", "dummy");
        product.setStartTime(getAfter(after));
        product.setEndTime(getBefore(before));
        return product;
    }

}