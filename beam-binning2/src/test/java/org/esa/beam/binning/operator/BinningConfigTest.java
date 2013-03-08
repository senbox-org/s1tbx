/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.binning.CompositingType;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.aggregators.AggregatorAverageML;
import org.esa.beam.binning.aggregators.AggregatorMinMax;
import org.esa.beam.binning.aggregators.AggregatorOnMaxSet;
import org.esa.beam.binning.support.PlateCarreeGrid;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.util.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class BinningConfigTest {

    private BinningConfig config;

    @Before
    public void initBinningConfig() throws IOException, BindingException {
        config = loadConfig("BinningConfigTest.xml");
    }

    @Test
    public void testPlanetaryGrid() {
        BinningConfig localConfig = new BinningConfig();
        PlanetaryGrid grid = localConfig.createPlanetaryGrid();
        assertEquals(2160, grid.getNumRows());
        assertEquals(SEAGrid.class, grid.getClass());

        localConfig.setPlanetaryGrid("org.esa.beam.binning.support.PlateCarreeGrid");
        localConfig.setNumRows(2000);
        grid = localConfig.createPlanetaryGrid();
        assertEquals(2000, grid.getNumRows());
        assertEquals(PlateCarreeGrid.class, grid.getClass());

        grid = config.createPlanetaryGrid();
        assertEquals(4320, grid.getNumRows());
        assertEquals(SEAGrid.class, grid.getClass());
    }

    @Test
    public void testCompositingType() throws Exception {
        assertEquals(CompositingType.MOSAICKING, config.getCompositingType());
    }

    @Test
    public void testResultingVariableContext() {
        VariableContext variableContext = config.createVariableContext();

        assertEquals(8, variableContext.getVariableCount());

        assertEquals(0, variableContext.getVariableIndex("ndvi"));
        assertEquals(1, variableContext.getVariableIndex("tsm"));
        assertEquals(2, variableContext.getVariableIndex("algal1"));
        assertEquals(3, variableContext.getVariableIndex("algal2"));
        assertEquals(4, variableContext.getVariableIndex("chl"));
        assertEquals(5, variableContext.getVariableIndex("reflec_3"));
        assertEquals(6, variableContext.getVariableIndex("reflec_7"));
        assertEquals(7, variableContext.getVariableIndex("reflec_8"));
        assertEquals(-1, variableContext.getVariableIndex("reflec_6"));
        assertEquals(-1, variableContext.getVariableIndex("reflec_10"));

        assertEquals("!l2_flags.INVALID && l2_flags.WATER", variableContext.getValidMaskExpression());

        assertEquals("ndvi", variableContext.getVariableName(0));
        assertEquals("(reflec_10 - reflec_6) / (reflec_10 + reflec_6)", variableContext.getVariableExpression(0));

        assertEquals("algal2", variableContext.getVariableName(3));
        assertEquals(null, variableContext.getVariableExpression(3));

        assertEquals("reflec_7", variableContext.getVariableName(6));
        assertEquals(null, variableContext.getVariableExpression(6));
    }

    @Test
    public void testResultingBinManager() {
        BinManager binManager = config.createBinningContext().getBinManager();
        assertEquals(6, binManager.getAggregatorCount());

        assertEquals(AggregatorAverage.class, binManager.getAggregator(0).getClass());
        assertArrayEquals(new String[]{"tsm_mean", "tsm_sigma"}, binManager.getAggregator(0).getOutputFeatureNames());
        assertEquals(-1.0F, binManager.getAggregator(0).getOutputFillValue(), 1E-05F);

        assertEquals(AggregatorAverageML.class, binManager.getAggregator(1).getClass());
        assertArrayEquals(new String[]{"algal1_mean", "algal1_sigma", "algal1_median", "algal1_mode"},
                          binManager.getAggregator(1).getOutputFeatureNames());
        assertTrue(Float.isNaN(binManager.getAggregator(1).getOutputFillValue()));

        assertEquals(AggregatorAverageML.class, binManager.getAggregator(2).getClass());
        assertArrayEquals(new String[]{"algal2_mean", "algal2_sigma", "algal2_median", "algal2_mode"},
                          binManager.getAggregator(2).getOutputFeatureNames());
        assertTrue(Float.isNaN(binManager.getAggregator(2).getOutputFillValue()));

        assertEquals(AggregatorAverageML.class, binManager.getAggregator(3).getClass());
        assertArrayEquals(new String[]{"chl_mean", "chl_sigma", "chl_median", "chl_mode"},
                          binManager.getAggregator(3).getOutputFeatureNames());
        assertEquals(-999.0F, binManager.getAggregator(3).getOutputFillValue(), 1E-05F);

        assertEquals(AggregatorOnMaxSet.class, binManager.getAggregator(4).getClass());
        assertArrayEquals(new String[]{"ndvi_max", "ndvi_mjd", "reflec_3", "reflec_7", "reflec_8"},
                          binManager.getAggregator(4).getOutputFeatureNames());
        assertTrue(Float.isNaN(binManager.getAggregator(4).getOutputFillValue()));

        assertEquals(AggregatorMinMax.class, binManager.getAggregator(5).getClass());
        assertArrayEquals(new String[]{"chl_min", "chl_max"}, binManager.getAggregator(5).getOutputFeatureNames());
        assertTrue(Float.isNaN(binManager.getAggregator(5).getOutputFillValue()));
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
    }

    @Test
    public void testNumRows() {
        assertEquals(4320, config.getNumRows());
    }

    private BinningConfig loadConfig(String configPath) throws IOException, BindingException {
        final InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(configPath));
        try {
            return BinningConfig.fromXml(FileUtils.readText(reader));
        } finally {
            reader.close();
        }
    }

}
