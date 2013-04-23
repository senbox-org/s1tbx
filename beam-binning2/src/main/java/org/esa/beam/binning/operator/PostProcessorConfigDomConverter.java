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

package org.esa.beam.binning.operator;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.Assert;
import org.esa.beam.binning.PostProcessorConfig;
import org.esa.beam.binning.PostProcessorDescriptor;
import org.esa.beam.binning.PostProcessorDescriptorRegistry;

/**
 * @author Norman Fomferra
 */
public class PostProcessorConfigDomConverter implements DomConverter {

    private DefaultDomConverter childConverter;

    public PostProcessorConfigDomConverter() {
        this.childConverter = new DefaultDomConverter(PostProcessorConfig.class);
    }

    @Override
    public Class<?> getValueType() {
        return PostProcessorConfig.class;
    }

    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException {
        DomElement typeElement = parentElement.getChild("type");
        String postProcessorName = typeElement.getValue();
        PostProcessorConfig postProcessorConfig = createPostProcessorConfig(postProcessorName);
        childConverter.convertDomToValue(parentElement, postProcessorConfig);
        return postProcessorConfig;
    }

    private PostProcessorConfig createPostProcessorConfig(String postProcessorName) {
        Assert.notNull(postProcessorName, "postProcessorName");
        final PostProcessorDescriptor descriptor = PostProcessorDescriptorRegistry.getInstance().getPostProcessorDescriptor(postProcessorName);
        Assert.argument(descriptor != null, String.format("Unknown postProcessor name '%s'", postProcessorName));
        return descriptor.createPostProcessorConfig();
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        PostProcessorConfig postProcessorConfig = (PostProcessorConfig) value;
        childConverter.convertValueToDom(postProcessorConfig, parentElement);
    }
}
