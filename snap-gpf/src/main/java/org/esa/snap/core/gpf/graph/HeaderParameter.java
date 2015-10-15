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
package org.esa.snap.core.gpf.graph;

import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.dom.DomConverter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.esa.snap.core.util.StringUtils;

public class HeaderParameter {
    private String name;
    private String type;

    private String value;
    private String defaultValue;
    private String description;
    private String label;
    private String unit;
    private String interval;
    private String[] valueSet;
    private String condition;
    private String pattern;
    private String format;
    private boolean notNull;
    private boolean notEmpty;
    private Class<? extends Validator> validator;
    private Class<? extends Converter> converter;
    private Class<? extends DomConverter> domConverter;
    private String itemAlias;
    private boolean itemsInlined;
    
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public String getUnit() {
        return unit;
    }
    public void setUnit(String unit) {
        this.unit = unit;
    }
    public String getInterval() {
        return interval;
    }
    public void setInterval(String interval) {
        this.interval = interval;
    }
    public String[] getValueSet() {
        return valueSet;
    }
    public void setValueSet(String[] valueSet) {
        this.valueSet = valueSet;
    }
    public String getCondition() {
        return condition;
    }
    public void setCondition(String condition) {
        this.condition = condition;
    }
    public String getPattern() {
        return pattern;
    }
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }
    public boolean isNotNull() {
        return notNull;
    }
    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }
    public boolean isNotEmpty() {
        return notEmpty;
    }
    public void setNotEmpty(boolean notEmpty) {
        this.notEmpty = notEmpty;
    }
    public Class<? extends Validator> getValidator() {
        return validator;
    }
    public void setValidator(Class<? extends Validator> validator) {
        this.validator = validator;
    }
    public Class<? extends Converter> getConverter() {
        return converter;
    }
    public void setConverter(Class<? extends Converter> converter) {
        this.converter = converter;
    }
    public Class<? extends DomConverter> getDomConverter() {
        return domConverter;
    }
    public void setDomConverter(Class<? extends DomConverter> domConverter) {
        this.domConverter = domConverter;
    }
    public String getItemAlias() {
        return itemAlias;
    }
    public void setItemAlias(String itemAlias) {
        this.itemAlias = itemAlias;
    }
    public boolean isItemsInlined() {
        return itemsInlined;
    }
    public void setItemsInlined(boolean itemsInlined) {
        this.itemsInlined = itemsInlined;
    }
    
    public static class Converter implements com.thoughtworks.xstream.converters.Converter {

        public boolean canConvert(Class aClass) {
            return HeaderParameter.class.equals(aClass);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            HeaderParameter headerParameter = (HeaderParameter) source;
            writer.addAttribute("name", headerParameter.getName());
            writer.addAttribute("type", headerParameter.getType());
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
        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {
            HeaderParameter headerParameter = new HeaderParameter();

            headerParameter.setName(reader.getAttribute("name"));
            headerParameter.setType(reader.getAttribute("type"));
            headerParameter.setDefaultValue(reader.getAttribute("defaultValue"));
            headerParameter.setDescription(reader.getAttribute("description"));
            headerParameter.setLabel(reader.getAttribute("label"));
            headerParameter.setUnit(reader.getAttribute("unit"));
            headerParameter.setInterval(reader.getAttribute("interval"));
            final String valueSetString = reader.getAttribute("valueSet");
            if(valueSetString != null) {
                headerParameter.setValueSet(StringUtils.toStringArray(valueSetString, ","));
            }
            headerParameter.setCondition(reader.getAttribute("condition"));
            headerParameter.setPattern(reader.getAttribute("pattern"));
            headerParameter.setFormat(reader.getAttribute("format"));
            headerParameter.setNotNull(Boolean.parseBoolean(reader.getAttribute("notNull")));
            headerParameter.setNotEmpty(Boolean.parseBoolean(reader.getAttribute("notEmpty")));

            headerParameter.setValue(reader.getValue());

            return headerParameter;
        }
    }
    
}
