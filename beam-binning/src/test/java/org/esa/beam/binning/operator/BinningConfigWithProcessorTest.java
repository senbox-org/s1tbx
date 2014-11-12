/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator;

import com.bc.ceres.binding.BindingException;
import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.aggregators.AggregatorMinMax;
import org.esa.beam.binning.aggregators.AggregatorOnMaxSet;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinningConfigWithProcessorTest {

    private BinningConfig config;

    @Before
    public void initBinningConfig() throws Exception {
        config = BinningConfigTest.loadConfig("BinningConfigWithProcessorTest.xml");
    }

    @Test
    public void testResultingBinManager() {
        BinManager binManager = config.createBinningContext(null, null, null).getBinManager();
        assertEquals(3, binManager.getAggregatorCount());

        assertEquals(AggregatorAverage.class, binManager.getAggregator(0).getClass());
        assertArrayEquals(new String[]{"tsm_mean", "tsm_sigma"}, binManager.getAggregator(0).getOutputFeatureNames());

        assertEquals(AggregatorOnMaxSet.class, binManager.getAggregator(1).getClass());
        assertArrayEquals(new String[]{"ndvi_max", "ndvi_mjd", "reflec_3", "reflec_7", "reflec_8"},
                binManager.getAggregator(1).getOutputFeatureNames());

        assertEquals(AggregatorMinMax.class, binManager.getAggregator(2).getClass());
        assertArrayEquals(new String[]{"chl_min", "chl_max"}, binManager.getAggregator(2).getOutputFeatureNames());

        assertArrayEquals(new String[]{"tsm_mean", "tsm_sigma", "chl_min", "cmax"}, binManager.getResultFeatureNames());
        assertTrue(binManager.hasPostProcessor());
    }

    @Test
    public void testXmlGeneration() throws BindingException {
        final String xml = config.toXml();
        //System.out.println("xml = \n" + xml);
        final BinningConfig configCopy = BinningConfig.fromXml(xml);

        assertEquals(config.getNumRows(), configCopy.getNumRows());
        assertEquals(config.getCompositingType(), configCopy.getCompositingType());
        assertEquals(config.getSuperSampling(), configCopy.getSuperSampling());
        assertEquals(config.getMaskExpr(), configCopy.getMaskExpr());
        assertArrayEquals(config.getVariableConfigs(), configCopy.getVariableConfigs());
        assertArrayEquals(config.getAggregatorConfigs(), configCopy.getAggregatorConfigs());
        assertEquals(config.getPostProcessorConfig(), configCopy.getPostProcessorConfig());
    }

}
