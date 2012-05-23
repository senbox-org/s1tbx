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

import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class BinningModelImplTest {

    @Test
    public void testSetGetProperty() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();
        binningModel.setProperty("key", new Float[]{2.0f, 3.0f});
        binningModel.setProperty("key2", new Integer[]{10, 20, 30});

        assertArrayEquals(new Product[0], binningModel.getSourceProducts());
        assertArrayEquals(new Float[]{2.0f, 3.0f}, (Float[])binningModel.getPropertyValue("key"));
        assertArrayEquals(new Integer[]{10, 20, 30}, (Integer[]) binningModel.getPropertyValue("key2"));
    }

    @Test
    public void testVariableConfigurationProperty() throws Exception {
        final BinningModel binningModel = new BinningModelImpl();
        assertArrayEquals(new TableRow[0], binningModel.getTableRows());

        final TableRow tableRow = new TableRow("name", "name", null, 0.1, 0.2f);
        binningModel.setProperty(BinningModel.PROPERTY_KEY_VARIABLE_CONFIGS,
                                 new TableRow[]{tableRow});

        assertArrayEquals(new TableRow[]{tableRow}, binningModel.getTableRows());
    }

    @Test
    public void testListening() throws Exception {
        final BinningModel binningModel = new BinningModelImpl();
        final MyPropertyChangeListener listener = new MyPropertyChangeListener();
        binningModel.addPropertyChangeListener(listener);
        
        binningModel.setProperty("key1", "value1");
        binningModel.setProperty("key2", "value2");
        
        assertEquals("value1", listener.targetMap.get("key1"));
        assertEquals("value2", listener.targetMap.get("key2"));
    }

    @Test
    public void testGetStartDate() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();
        binningModel.setProperty(BinningModel.PROPERTY_KEY_TEMPORAL_FILTER, false);

        assertNull(binningModel.getStartDate());

        binningModel.setProperty(BinningModel.PROPERTY_KEY_TEMPORAL_FILTER, true);
        binningModel.setProperty(BinningModel.PROPERTY_KEY_START_DATE, new GregorianCalendar(2000, 1, 1));

        assertNotNull(binningModel.getStartDate());
        SimpleDateFormat dateFormat = new SimpleDateFormat(BinningOp.DATE_PATTERN);
        String expectedString = dateFormat.format(new GregorianCalendar(2000, 1, 1).getTime());
        assertEquals(expectedString, binningModel.getStartDate());
    }

    @Test
    public void testGetEndDate() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();
        binningModel.setProperty(BinningModel.PROPERTY_KEY_TEMPORAL_FILTER, false);

        assertNull(binningModel.getEndDate());

        binningModel.setProperty(BinningModel.PROPERTY_KEY_TEMPORAL_FILTER, true);
        binningModel.setProperty(BinningModel.PROPERTY_KEY_END_DATE, new GregorianCalendar(2010, 0, 1));

        assertNotNull(binningModel.getEndDate());
        SimpleDateFormat dateFormat = new SimpleDateFormat(BinningOp.DATE_PATTERN);
        String expectedString = dateFormat.format(new GregorianCalendar(2010, 0, 1).getTime());
        assertEquals(expectedString, binningModel.getEndDate());
    }

    @Test
    public void testGetValidExpression() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();
        assertNull(binningModel.getValidExpression());
        binningModel.setProperty(BinningModel.PROPERTY_KEY_EXPRESSION, "some_expression");

        assertEquals("some_expression", binningModel.getValidExpression());
    }

    @Test
    public void testGetOutputBinnedData() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();
        assertFalse(binningModel.shallOutputBinnedData());
        binningModel.setProperty(BinningModel.PROPERTY_KEY_OUTPUT_BINNED_DATA, true);
        assertTrue(binningModel.shallOutputBinnedData());
    }

    @Test
    public void testGetSuperSampling() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();
        assertEquals(1, binningModel.getSuperSampling());

        binningModel.setProperty(BinningModel.PROPERTY_KEY_SUPERSAMPLING, 10);
        assertEquals(10, binningModel.getSuperSampling());
    }

    @Test
    public void testGetNumRows() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();
        assertEquals(2160, binningModel.getNumRows());

        binningModel.setProperty(BinningModel.PROPERTY_KEY_TARGET_HEIGHT, 2000);
        assertEquals(2000, binningModel.getNumRows());
    }

    @Test
    public void testGetRegion_Fail() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();
        try {
            binningModel.getRegion();
            fail();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().equals("Should never come here"));
        }
    }

    @Test
    public void testGetRegion_Global() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();

        binningModel.setProperty(BinningModel.PROPERTY_KEY_GLOBAL, true);
        assertEquals("polygon((-180 -90, 180 -90, 180 90, -180 90, -180 -90))", binningModel.getRegion());
    }

    @Test
    public void testGetRegion_Compute() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();

        binningModel.setProperty(BinningModel.PROPERTY_KEY_COMPUTE_REGION, true);
        assertNull(binningModel.getRegion());
    }

    @Test
    public void testGetRegion_WithSpecifiedRegion() throws Exception {
        final BinningModelImpl binningModel = new BinningModelImpl();

        binningModel.setProperty(BinningModel.PROPERTY_KEY_REGION, true);
        binningModel.setProperty(BinningFilterPanel.PROPERTY_NORTH_BOUND, 50.0);
        binningModel.setProperty(BinningFilterPanel.PROPERTY_EAST_BOUND, 15.0);
        binningModel.setProperty(BinningFilterPanel.PROPERTY_WEST_BOUND, 10.0);
        binningModel.setProperty(BinningFilterPanel.PROPERTY_SOUTH_BOUND, 40.0);

        final String region = binningModel.getRegion();
        assertEquals("POLYGON ((10 40, 10 50, 15 50, 15 40, 10 40))", region);
    }

    private static class MyPropertyChangeListener implements PropertyChangeListener {

        Map<String, Object> targetMap = new HashMap<String, Object>();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            targetMap.put(evt.getPropertyName(), evt.getNewValue());
        }
    }

}
