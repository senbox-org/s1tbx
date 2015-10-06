/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.descriptor;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.DefaultPropertySetDescriptor;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySetDescriptor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;

import java.util.Map;

public class PropertySetDescriptorFactory {

    public static PropertySetDescriptor createForOperator(OperatorDescriptor operatorDescriptor, Map<String, Product> sourceProductMap) throws ConversionException {
        DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
        ParameterDescriptor[] parameterDescriptors = operatorDescriptor.getParameterDescriptors();
        for (ParameterDescriptor parameterDescriptor : parameterDescriptors) {
            PropertyDescriptor propertyDescriptor = ParameterDescriptorFactory.convert(parameterDescriptor, sourceProductMap);
            propertySetDescriptor.addPropertyDescriptor(propertyDescriptor);
        }
        return propertySetDescriptor;
    }
}
