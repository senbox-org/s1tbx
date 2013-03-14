package org.esa.beam.statistics.percentile.interpolated;

import static org.junit.Assert.*;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.*;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public class UtilsTest_GroupProductsDaily {

    @Test
    public void testSomething() throws ParseException {
        final Product[] products = {
                    createProduct("2013-03-05 00:00:10"),
                    createProduct("2013-03-05 23:59:50"),
                    createProduct("2013-03-06 00:00:10"),
                    createProduct("2013-03-06 23:59:50"),
        };

        final TreeMap<Long, List<Product>> longListTreeMap = Utils.groupProductsDaily(products);

        assertEquals(2, longListTreeMap.size());
        final Long[] mjdKeys = longListTreeMap.keySet().toArray(new Long[2]);
        assertEquals("means 2013-03-05", 56356L, mjdKeys[0].longValue());
        assertEquals("means 2013-03-05", 56357L, mjdKeys[1].longValue());

        final List<Product> productList1 = longListTreeMap.get(mjdKeys[0]);
        assertEquals(2, productList1.size());
        assertSame(productList1.get(0), products[0]);
        assertSame(productList1.get(1), products[1]);

        final List<Product> productList2 = longListTreeMap.get(mjdKeys[1]);
        assertEquals(2, productList2.size());
        assertSame(productList2.get(0), products[2]);
        assertSame(productList2.get(1), products[3]);
    }

    private Product createProduct(String centerDateString) throws ParseException {
        final ProductData.UTC centerUTC = ProductData.UTC.parse(centerDateString, "yyyy-MM-dd hh:mm:ss");
        final Calendar centerCalendar = centerUTC.getAsCalendar();
        final long centerTime = centerCalendar.getTime().getTime();
        final ProductData.UTC startTime = ProductData.UTC.create(new Date(centerTime - 20), 0);
        final ProductData.UTC endTime = ProductData.UTC.create(new Date(centerTime + 20), 0);
        final Product product = new Product("n", "t", 2, 2);
        product.setStartTime(startTime);
        product.setEndTime(endTime);
        return product;
    }
}
