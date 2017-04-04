/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.core.gpf.descriptor.template;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Converter for XML serialization of templates.
 *
 * @author Cosmin Cara
 */
public class TemplateConverter implements Converter {
    private final Converter defaultConverter;
    private final ReflectionProvider reflectionProvider;

    public TemplateConverter(Converter defaultConverter, ReflectionProvider reflectionProvider) {
        this.defaultConverter = defaultConverter;
        this.reflectionProvider = reflectionProvider;
    }

    public boolean canConvert(Class type) {
        return Template.class.isAssignableFrom(type);
    }

    public void marshal(Object o, HierarchicalStreamWriter out, MarshallingContext context) {
        defaultConverter.marshal(o, out, context);
    }

    public Object unmarshal(HierarchicalStreamReader in, UnmarshallingContext context) {
        String type = in.getAttribute("type");
        Class<?> resultType =
                (type == null || "simple".equals(type)) ? MemoryTemplate.class : FileTemplate.class;
        Object result = reflectionProvider.newInstance(resultType);
        return context.convertAnother(result, resultType, defaultConverter);
    }
}
