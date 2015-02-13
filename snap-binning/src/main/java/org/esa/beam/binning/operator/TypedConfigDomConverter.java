/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.Assert;
import org.esa.beam.binning.TypedConfig;
import org.esa.beam.binning.TypedDescriptor;
import org.esa.beam.binning.TypedDescriptorsRegistry;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;

/**
 * @author Norman Fomferra
 * @author Marco Zuehlke
 * @see TypedConfig
 * @see TypedDescriptor
 */
public class TypedConfigDomConverter<TD extends TypedDescriptor, TC extends TypedConfig> implements DomConverter {

    private final Class<TD> descriptorClass;
    private final Class<TC> configClass;

    protected TypedConfigDomConverter(Class<TD> descriptorClass, Class<TC> configClass) {
        this.descriptorClass = descriptorClass;
        this.configClass = configClass;
    }

    @Override
    public Class<?> getValueType() {
        return configClass;
    }

    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException {
        DomElement typeElement = parentElement.getChild("type");
        if (typeElement == null) {
            throw new ConversionException(String.format("Missing element 'type' in parent element '%s'", parentElement.getName()));
        }
        String typeName = typeElement.getValue();
        TC config;
        if (value == null) {
            config = createConfig(typeName);
        } else {
            config = (TC) value;
        }
        DomConverter childConverter = createChildConverter(config.getClass());
        childConverter.convertDomToValue(parentElement, config);
        return config;
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        DomConverter childConverter = createChildConverter(value.getClass());
        childConverter.convertValueToDom(value, parentElement);
    }

    protected TC createConfig(String typeName) {
        Assert.notNull(typeName, "typeName");
        TypedDescriptor<TC> descriptor = TypedDescriptorsRegistry.getInstance().getDescriptor(descriptorClass, typeName);
        Assert.argument(descriptor != null, String.format("Unknown type name '%s'", typeName));
        return descriptor.createConfig();
    }

    private DomConverter createChildConverter(Class<?> actualConfigType) {
        return new DefaultDomConverter(actualConfigType, new ParameterDescriptorFactory());
    }

}
