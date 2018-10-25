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
package org.esa.snap.core.gpf.descriptor;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.core.Assert;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Default implementation of the {@link ParameterDescriptor} interface.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class DefaultParameterDescriptor implements ParameterDescriptor {
    String name;
    String alias;
    Class<?> dataType;
    String defaultValue;
    String description;
    String label;
    String unit;
    String interval;
    String[] valueSet;
    String condition;
    String pattern;
    String format;
    Boolean notNull;
    Boolean notEmpty;
    Class<? extends RasterDataNode> rasterDataNodeClass;
    Class<? extends Validator> validatorClass;
    Class<? extends Converter> converterClass;
    Class<? extends DomConverter> domConverterClass;
    String itemAlias;
    Boolean deprecated;

    public DefaultParameterDescriptor() {
    }

    public DefaultParameterDescriptor(String name, Class<?> dataType) {
        Assert.notNull(name, "name");
        Assert.notNull(dataType, "dataType");
        this.name = name;
        this.dataType = dataType;
        this.valueSet = new String[0];
    }

    private Object readResolve() {
        // todo - write test that assert the following
        //        if (this.dataType == null) {
        //            this.dataType = Map.class;
        //        }
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Class<?> getDataType() {
        return dataType;
    }

    public void setDataType(Class<?> dataType) {
        this.dataType = dataType;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    @Override
    public String[] getValueSet() {
        return valueSet != null ? valueSet : new String[0];
    }

    public void setValueSet(String[] valueSet) {
        this.valueSet = valueSet;
    }

    @Override
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public boolean isNotNull() {
        return notNull != null ? notNull : false;
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    @Override
    public boolean isNotEmpty() {
        return notEmpty != null ? notEmpty : false;
    }

    @Override
    public boolean isDeprecated() {
        return deprecated != null ? deprecated : false;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public void setNotEmpty(boolean notEmpty) {
        this.notEmpty = notEmpty;
    }

    @Override
    public Class<? extends RasterDataNode> getRasterDataNodeClass() {
        return rasterDataNodeClass;
    }

    public void setRasterDataNodeClass(Class<? extends RasterDataNode> rasterDataNodeClass) {
        this.rasterDataNodeClass = rasterDataNodeClass;
    }

    @Override
    public Class<? extends Validator> getValidatorClass() {
        return validatorClass;
    }

    public void setValidatorClass(Class<? extends Validator> validator) {
        this.validatorClass = validator;
    }

    @Override
    public Class<? extends Converter> getConverterClass() {
        return converterClass;
    }

    public void setConverterClass(Class<? extends Converter> converter) {
        this.converterClass = converter;
    }

    @Override
    public Class<? extends DomConverter> getDomConverterClass() {
        return domConverterClass;
    }

    public void setDomConverterClass(Class<? extends DomConverter> domConverter) {
        this.domConverterClass = domConverter;
    }

    @Override
    public String getItemAlias() {
        return itemAlias;
    }

    public void setItemAlias(String itemAlias) {
        this.itemAlias = itemAlias;
    }

    @Override
    public boolean isStructure() {
        return isStructure(getDataType());
    }

    @Override
    public ParameterDescriptor[] getStructureMemberDescriptors() {
        return getDataMemberDescriptors(getDataType());
    }

    public static boolean isStructure(Class<?> type) {
        return !isSimple(type) && !type.isArray();
    }

    public static boolean isSimple(Class<?> type) {
        return type.isPrimitive()
               || Boolean.class.isAssignableFrom(type)
               || Character.class.isAssignableFrom(type)
               || Number.class.isAssignableFrom(type)
               || CharSequence.class.isAssignableFrom(type)
               || ConverterRegistry.getInstance().getConverter(type) != null;
    }

    public static ParameterDescriptor[] getDataMemberDescriptors(Class<?> dataType) {
        if (!isStructure(dataType)) {
            return new ParameterDescriptor[0];
        }
        ArrayList<ParameterDescriptor> parameterDescriptors = new ArrayList<>();
        Field[] declaredFields = dataType.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            int modifiers = declaredField.getModifiers();
            if (!(Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers))) {
                Parameter annotation = declaredField.getAnnotation(Parameter.class);
                if (annotation != null) {
                    boolean isDeprecated = declaredField.getAnnotation(Deprecated.class) != null;
                    parameterDescriptors.add(new AnnotationParameterDescriptor(declaredField.getName(), declaredField.getType(), isDeprecated,
                                                                               annotation));
                } else {
                    parameterDescriptors.add(new DefaultParameterDescriptor(declaredField.getName(), declaredField.getType()));
                }
            }
        }
        return parameterDescriptors.toArray(new ParameterDescriptor[parameterDescriptors.size()]);
    }

    /**
     * @deprecated Class has no usage and has no API doc. It might be deleted without further notice.
     */
    @Deprecated
    public static class XStreamConverter implements com.thoughtworks.xstream.converters.Converter {

        public boolean canConvert(Class aClass) {
            return DefaultParameterDescriptor.class.equals(aClass);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            DefaultParameterDescriptor headerParameter = (DefaultParameterDescriptor) source;
            writer.addAttribute("name", headerParameter.getName());
            writer.addAttribute("dataType", headerParameter.getDataType().toString());
            writer.addAttribute("defaultValue", headerParameter.getDefaultValue());
            writer.addAttribute("description", headerParameter.getDescription());
            writer.addAttribute("label", headerParameter.getLabel());
            writer.addAttribute("unit", headerParameter.getUnit());
            writer.addAttribute("interval", headerParameter.getInterval());
            writer.addAttribute("valueSet", StringUtils.arrayToString(headerParameter.getValueSet(), ","));
            writer.addAttribute("condition", headerParameter.getCondition());
            writer.addAttribute("pattern", headerParameter.getPattern());
            writer.addAttribute("format", headerParameter.getFormat());
            writer.addAttribute("notNull", String.valueOf(headerParameter.isNotNull()));
            writer.addAttribute("notEmpty", String.valueOf(headerParameter.isNotEmpty()));
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            DefaultParameterDescriptor headerParameter = new DefaultParameterDescriptor();

            try {
                headerParameter.setDataType(Class.forName(reader.getAttribute("dataType")));
            } catch (ClassNotFoundException e) {
                throw new ConversionException(e);
            }
            headerParameter.setName(reader.getAttribute("name"));
            headerParameter.setDefaultValue(reader.getAttribute("defaultValue"));
            headerParameter.setDescription(reader.getAttribute("description"));
            headerParameter.setLabel(reader.getAttribute("label"));
            headerParameter.setUnit(reader.getAttribute("unit"));
            headerParameter.setInterval(reader.getAttribute("interval"));
            final String valueSetString = reader.getAttribute("valueSet");
            if (valueSetString != null) {
                headerParameter.setValueSet(StringUtils.toStringArray(valueSetString, ","));
            }
            headerParameter.setCondition(reader.getAttribute("condition"));
            headerParameter.setPattern(reader.getAttribute("pattern"));
            headerParameter.setFormat(reader.getAttribute("format"));
            headerParameter.setNotNull(Boolean.parseBoolean(reader.getAttribute("notNull")));
            headerParameter.setNotEmpty(Boolean.parseBoolean(reader.getAttribute("notEmpty")));

            return headerParameter;
        }
    }

}
