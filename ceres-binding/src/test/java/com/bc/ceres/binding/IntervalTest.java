package com.bc.ceres.binding;

import junit.framework.TestCase;

public class IntervalTest extends TestCase {
    public void testParseFailures()  {
        try {
            Interval.parseInterval(null);
            fail();
        } catch (NullPointerException e) {
        } catch (ConversionException e) {
            fail();
        }
        try {
            Interval.parseInterval("");
            fail();
        } catch (ConversionException e) {
        }
        try {
            Interval.parseInterval("10,20");
            fail();
        } catch (ConversionException e) {
        }
    }

    public void testParse() throws ConversionException {
        Interval interval = Interval.parseInterval("[10,20)");
        assertEquals(10.0, interval.getMin(), 1e-10);
        assertEquals(true, interval.isMinIncluded());
        assertEquals(20.0, interval.getMax(), 1e-10);
        assertEquals(false, interval.isMaxIncluded());

        interval = Interval.parseInterval("(-10,20]");
        assertEquals(-10.0, interval.getMin(), 1e-10);
        assertEquals(false, interval.isMinIncluded());
        assertEquals(20.0, interval.getMax(), 1e-10);
        assertEquals(true, interval.isMaxIncluded());

        interval = Interval.parseInterval("(*, 20]");
        assertEquals(Double.NEGATIVE_INFINITY, interval.getMin(), 1e-10);
        assertEquals(false, interval.isMinIncluded());
        assertEquals(20.0, interval.getMax(), 1e-10);
        assertEquals(true, interval.isMaxIncluded());

        interval = Interval.parseInterval("[-10,*]");
        assertEquals(-10.0, interval.getMin(), 1e-10);
        assertEquals(true, interval.isMinIncluded());
        assertEquals(Double.POSITIVE_INFINITY, interval.getMax(), 1e-10);
        assertEquals(true, interval.isMaxIncluded());
    }

    public void testContains() {

        Interval interval = new Interval(-1.5, 3.2, true, false);

        assertEquals(false, interval.contains(-1.6));
        assertEquals(true, interval.contains(-1.5));
        assertEquals(true, interval.contains(-1.4));

        assertEquals(true, interval.contains(3.1));
        assertEquals(false, interval.contains(3.2));
        assertEquals(false, interval.contains(3.3));

        interval = new Interval(-1.5, 3.2, false, true);

        assertEquals(false, interval.contains(-1.6));
        assertEquals(false, interval.contains(-1.5));
        assertEquals(true, interval.contains(-1.4));

        assertEquals(true, interval.contains(3.1));
        assertEquals(true, interval.contains(3.2));
        assertEquals(false, interval.contains(3.3));

        interval = new Interval(Double.NEGATIVE_INFINITY, 3.2, false, true);

        assertEquals(true, interval.contains(-1.6));
        assertEquals(true, interval.contains(-1.5));
        assertEquals(true, interval.contains(-1.4));

        assertEquals(true, interval.contains(3.1));
        assertEquals(true, interval.contains(3.2));
        assertEquals(false, interval.contains(3.3));

        interval = new Interval(-1.5, Double.POSITIVE_INFINITY, false, true);

        assertEquals(false, interval.contains(-1.6));
        assertEquals(false, interval.contains(-1.5));
        assertEquals(true, interval.contains(-1.4));

        assertEquals(true, interval.contains(3.1));
        assertEquals(true, interval.contains(3.2));
        assertEquals(true, interval.contains(3.3));
    }

}
