package org.esa.beam.binning.aggregators;


import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class AggregatorBiasCorrectDateIndexTest {

    @Test
    public void testGetDateIndex() {
        final AggregatorBiasCorrect.DateIndex dateIndex = new AggregatorBiasCorrect.DateIndex(2009, 2011);

        int idx = dateIndex.get(55146.7756);    // 2009-11
        assertEquals(10, idx);                  // year 0, month 10

        idx = dateIndex.get(54842.2842);        // 2009-01
        assertEquals(0, idx);                   // year 0, month 0

        idx = dateIndex.get(55197.1007);        // 2010-01
        assertEquals(12, idx);                  // year 1, month 0

        idx = dateIndex.get(55672.3245);        // 2011-04
        assertEquals(27, idx);                  // year 2, month 3
    }

    @Test
    public void testGetDateIndex_outOfRange() {
        final AggregatorBiasCorrect.DateIndex dateIndex = new AggregatorBiasCorrect.DateIndex(2009, 2011);

        int idx = dateIndex.get(54830.3762);    // 2008-12
        assertEquals(-1, idx);                  // out of range

        idx = dateIndex.get(55972.6734);        // 2012-02
        assertEquals(-1, idx);                  // out of range
    }
}
