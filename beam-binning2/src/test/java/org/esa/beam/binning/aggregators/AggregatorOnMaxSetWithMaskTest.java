/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.aggregators;


import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.MyVariableContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.esa.beam.binning.aggregators.AggregatorTestUtils.createCtx;
import static org.junit.Assert.assertEquals;

public class AggregatorOnMaxSetWithMaskTest {

    BinContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = createCtx();
    }

    @Test
    public void testMetadata() {
        AggregatorOnMaxSetWithMask agg = new AggregatorOnMaxSetWithMask(new MyVariableContext("a", "b", "c"), "c", "a", "b");

        assertEquals("ON_MAX_SET_WITH_MASK", agg.getName());

        assertEquals(4, agg.getSpatialFeatureNames().length);
        assertEquals("c_max", agg.getSpatialFeatureNames()[0]);
        assertEquals("c_mjd", agg.getSpatialFeatureNames()[1]);
        assertEquals("c_count", agg.getSpatialFeatureNames()[2]);
        assertEquals("b", agg.getSpatialFeatureNames()[3]);

        assertEquals(4, agg.getTemporalFeatureNames().length);
        assertEquals("c_max", agg.getTemporalFeatureNames()[0]);
        assertEquals("c_mjd", agg.getTemporalFeatureNames()[1]);
        assertEquals("c_count", agg.getTemporalFeatureNames()[2]);
        assertEquals("b", agg.getTemporalFeatureNames()[3]);

        assertEquals(4, agg.getOutputFeatureNames().length);
        assertEquals("c_max", agg.getOutputFeatureNames()[0]);
        assertEquals("c_mjd", agg.getOutputFeatureNames()[1]);
        assertEquals("c_count", agg.getOutputFeatureNames()[2]);
        assertEquals("b", agg.getOutputFeatureNames()[3]);
    }

    @Test
    public void testMetadataWithEmptySetNames() {
        AggregatorOnMaxSetWithMask agg = new AggregatorOnMaxSetWithMask(new MyVariableContext("a", "b", "c"), "c", "a");

        assertEquals("ON_MAX_SET_WITH_MASK", agg.getName());

        assertEquals(3, agg.getSpatialFeatureNames().length);
        assertEquals("c_max", agg.getSpatialFeatureNames()[0]);
        assertEquals("c_mjd", agg.getSpatialFeatureNames()[1]);
        assertEquals("c_count", agg.getSpatialFeatureNames()[2]);

        assertEquals(3, agg.getTemporalFeatureNames().length);
        assertEquals("c_max", agg.getTemporalFeatureNames()[0]);
        assertEquals("c_mjd", agg.getTemporalFeatureNames()[1]);
        assertEquals("c_count", agg.getTemporalFeatureNames()[2]);

        assertEquals(3, agg.getOutputFeatureNames().length);
        assertEquals("c_max", agg.getOutputFeatureNames()[0]);
        assertEquals("c_mjd", agg.getOutputFeatureNames()[1]);
        assertEquals("c_count", agg.getOutputFeatureNames()[2]);
    }

}
