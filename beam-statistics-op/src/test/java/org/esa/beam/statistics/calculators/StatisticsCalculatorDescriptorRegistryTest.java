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

package org.esa.beam.statistics.calculators;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class StatisticsCalculatorDescriptorRegistryTest {

    @Test
    public void testRegistry() throws Exception {
        final StatisticsCalculatorDescriptorRegistry instance = StatisticsCalculatorDescriptorRegistry.getInstance();
        final StatisticsCalculatorDescriptor[] descriptors = instance.getStatisticCalculatorDescriptors();
        assertEquals(1, descriptors.length);
        assertTrue(descriptors[0] instanceof StatisticsCalculatorPercentile.Descriptor);
        final PropertyContainer propertySet = new PropertyContainer();
        final Property percentileProperty = new Property(new PropertyDescriptor("percentile", Integer.class), new DefaultPropertyAccessor());
        percentileProperty.setValue(5);
        propertySet.addProperty(percentileProperty);
        final StatisticsCalculator statisticsCalculator = descriptors[0].createStatisticsCalculator(propertySet);
        assertTrue(statisticsCalculator instanceof StatisticsCalculatorPercentile);
        assertEquals(5, ((StatisticsCalculatorPercentile)statisticsCalculator).percentile);
    }
}
