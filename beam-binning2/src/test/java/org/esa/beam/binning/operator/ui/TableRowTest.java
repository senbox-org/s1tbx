/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.binning.operator.ui;

import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class TableRowTest {

    @Test
    public void testTableRow_1() throws Exception {
        AggregatorAverage.Descriptor aggregator = new AggregatorAverage.Descriptor();
        TableRow tableRow = new TableRow("<my_name>", "", aggregator, 10.0, 5);
        assertEquals("my_name", tableRow.name);
        assertNull(tableRow.expression);
        assertSame(aggregator, tableRow.aggregator);
        assertEquals(10.0, tableRow.weight, 1E5);
        assertEquals((Integer) 5, tableRow.percentile);
    }

    @Test
    public void testTableRow_2() throws Exception {
        AggregatorAverage.Descriptor aggregator = new AggregatorAverage.Descriptor();
        String constantName = "my_untouched_name";
        String constantExpression = "PI * x + y";
        TableRow tableRow = new TableRow(constantName, constantExpression, aggregator, 10.0, 5);
        assertEquals(constantName, tableRow.name);
        assertEquals(constantExpression, tableRow.expression);
        assertSame(aggregator, tableRow.aggregator);
        assertEquals(10.0, tableRow.weight, 1E5);
        assertEquals((Integer) 5, tableRow.percentile);
    }
}
