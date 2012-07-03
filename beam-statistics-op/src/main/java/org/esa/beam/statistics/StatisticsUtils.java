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

package org.esa.beam.statistics;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;

/**
 * @author Thomas Storm
 */
class StatisticsUtils {

    static PropertySet createPropertySet(StatisticsOp.BandConfiguration bandConfiguration) {
        final PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(new Property(new PropertyDescriptor("percentile", Integer.class), new DefaultPropertyAccessor()));
        propertyContainer.addProperty(new Property(new PropertyDescriptor("weightCoeff", Double.class), new DefaultPropertyAccessor()));
        try {
            propertyContainer.getProperty("percentile").setValue(bandConfiguration.percentile);
            propertyContainer.getProperty("weightCoeff").setValue(bandConfiguration.weightCoeff);
        } catch (ValidationException e) {
            // Can never come here
            throw new IllegalStateException(e);
        }
        return propertyContainer;
    }
}
