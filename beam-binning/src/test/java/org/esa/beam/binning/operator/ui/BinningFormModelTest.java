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

package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static org.esa.beam.binning.operator.ui.BinningFormModel.*;
import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class BinningFormModelTest {

    @Test
    public void testSetGetProperty() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();
        PropertySet propertySet = binningFormModel.getBindingContext().getPropertySet();
        propertySet.addProperty(createProperty("key", Float[].class));
        propertySet.addProperty(createProperty("key2", Integer[].class));

        binningFormModel.setProperty("key", new Float[]{2.0f, 3.0f});
        binningFormModel.setProperty("key2", new Integer[]{10, 20, 30});

        assertArrayEquals(new Product[0], binningFormModel.getSourceProducts());
        assertArrayEquals(new Float[]{2.0f, 3.0f}, (Float[]) binningFormModel.getPropertyValue("key"));
        assertArrayEquals(new Integer[]{10, 20, 30}, (Integer[]) binningFormModel.getPropertyValue("key2"));
    }

    @Test
    public void testAggregatorConfigsProperty() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();
        assertArrayEquals(new AggregatorConfig[0], binningFormModel.getAggregatorConfigs());

        final AggregatorConfig aggConf1 = new AggregatorAverage.Config("x", "y", 0.4, true, false);
        final AggregatorConfig aggConf2 = new AggregatorAverage.Config("a", "b", 0.6, false, null);
        binningFormModel.setProperty(PROPERTY_KEY_AGGREGATOR_CONFIGS, new AggregatorConfig[]{aggConf1, aggConf2});

        assertArrayEquals(new AggregatorConfig[]{aggConf1, aggConf2}, binningFormModel.getAggregatorConfigs());
    }

    @Test
    public void testVariableConfigsProperty() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();
        assertArrayEquals(new VariableConfig[0], binningFormModel.getVariableConfigs());

        final VariableConfig varConf = new VariableConfig();
        varConf.setName("prefix");
        varConf.setExpr("NOT algal_2");
        binningFormModel.setProperty(PROPERTY_KEY_VARIABLE_CONFIGS, new VariableConfig[]{varConf});

        assertArrayEquals(new VariableConfig[]{varConf}, binningFormModel.getVariableConfigs());
    }

    @Test
    public void testListening() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();
        PropertySet propertySet = binningFormModel.getBindingContext().getPropertySet();
        propertySet.addProperty(createProperty("key1", String.class));
        propertySet.addProperty(createProperty("key2", String.class));
        final MyPropertyChangeListener listener = new MyPropertyChangeListener();
        binningFormModel.addPropertyChangeListener(listener);

        binningFormModel.setProperty("key1", "value1");
        binningFormModel.setProperty("key2", "value2");

        assertEquals("value1", listener.targetMap.get("key1"));
        assertEquals("value2", listener.targetMap.get("key2"));
    }

    @Test
    public void testGetStartDate() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_TIME_FILTER_METHOD, BinningOp.TimeFilterMethod.NONE);

        assertNull(binningFormModel.getStartDateTime());

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_TIME_FILTER_METHOD, BinningOp.TimeFilterMethod.TIME_RANGE);
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_START_DATE_TIME, "2000-01-01");

        assertNotNull(binningFormModel.getStartDateTime());
        SimpleDateFormat dateFormat = new SimpleDateFormat(BinningOp.DATE_INPUT_PATTERN);
        String expectedString = dateFormat.format(new GregorianCalendar(2000, 0, 1).getTime());
        assertEquals(expectedString, binningFormModel.getStartDateTime());
    }

    @Test
    public void testGetValidExpression() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();
        assertTrue(Boolean.parseBoolean(binningFormModel.getMaskExpr()));
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_MASK_EXPR, "some_expression");

        assertEquals("some_expression", binningFormModel.getMaskExpr());
    }

    @Test
    public void testGetSuperSampling() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();
        assertEquals(1, binningFormModel.getSuperSampling());

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING, 10);
        assertEquals(10, binningFormModel.getSuperSampling());
    }

    @Test
    public void testGetNumRows() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();
        assertEquals(2160, binningFormModel.getNumRows());

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_NUM_ROWS, 2000);
        assertEquals(2000, binningFormModel.getNumRows());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetRegion_Fail() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();
        binningFormModel.getRegion();
    }

    @Test
    public void testGetRegion_Global() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_GLOBAL, true);
        assertEquals("POLYGON ((-180 -90, 180 -90, 180 90, -180 90, -180 -90))", binningFormModel.getRegion().toText());
    }

    @Test
    public void testGetRegion_Compute() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_COMPUTE_REGION, true);
        assertNull(binningFormModel.getRegion());
    }

    @Test
    public void testGetRegion_WithSpecifiedRegion() throws Exception {
        final BinningFormModel binningFormModel = new BinningFormModel();

        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_BOUNDS, true);
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_NORTH_BOUND, 50.0);
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_EAST_BOUND, 15.0);
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_WEST_BOUND, 10.0);
        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_SOUTH_BOUND, 40.0);

        assertEquals("POLYGON ((10 40, 10 50, 15 50, 15 40, 10 40))", binningFormModel.getRegion().toText());
    }

    private Property createProperty(String propertyName, Class<?> type) {
        return new Property(new PropertyDescriptor(propertyName, type), new DefaultPropertyAccessor());
    }

    private static class MyPropertyChangeListener implements PropertyChangeListener {

        Map<String, Object> targetMap = new HashMap<>();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            targetMap.put(evt.getPropertyName(), evt.getNewValue());
        }
    }

}
