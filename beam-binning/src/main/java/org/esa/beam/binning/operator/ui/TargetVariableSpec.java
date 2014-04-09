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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.util.StringUtils;

/**
 * @author thomas
 */
class TargetVariableSpec implements Cloneable {

    String targetName = "";
    Source source;
    String aggregationString = "";
    AggregatorDescriptor aggregatorDescriptor;
    PropertyContainer aggregatorProperties;

    TargetVariableSpec() {
        source = new Source();
        aggregatorProperties = new PropertyContainer();
    }

    TargetVariableSpec(TargetVariableSpec spec) {
        this.targetName = spec.targetName;
        this.source = new Source(spec.source);
        this.aggregationString = spec.aggregationString;
        if (StringUtils.isNullOrEmpty(spec.aggregationString)) {
            return;
        }
        this.aggregatorDescriptor = spec.aggregatorDescriptor; // using the same instance is ok
        this.aggregatorProperties = new PropertyContainer();
        for (Property property : spec.aggregatorProperties.getProperties()) {
            aggregatorProperties.addProperty(Property.create(property.getName(), property.getValue()));
        }
    }

    @Override
    public String toString() {
        String aggregation = aggregationString;
        if (StringUtils.isNullOrEmpty(aggregation)) {
            aggregation = "null";
        }
        return "{" +
               "source=" + source +
               ", targetName='" + targetName + '\'' +
               ", aggregation='" + aggregation +
               '}';
    }

    public boolean isValid() {
        boolean expressionCorrect = (source.type == Source.EXPRESSION_SOURCE_TYPE && source.expression != null && targetName != null) || source.type == Source.RASTER_SOURCE_TYPE;
        boolean bandCorrect = (source.type == Source.RASTER_SOURCE_TYPE && source.bandName != null) || source.type == Source.EXPRESSION_SOURCE_TYPE;
        boolean aggregationSet = StringUtils.isNotNullAndNotEmpty(aggregationString);
        return expressionCorrect && bandCorrect && aggregationSet;
    }

    static class Source {

        static final int RASTER_SOURCE_TYPE = 0;
        static final int EXPRESSION_SOURCE_TYPE = 1;

        int type;
        String bandName;
        String expression;

        Source() {
        }

        Source(Source source) {
            this();
            this.type = source.type;
            this.expression = source.expression;
            this.bandName = source.bandName;
        }

        @Override
        public String toString() {
            String s = type == RASTER_SOURCE_TYPE ? bandName : expression;
            if (StringUtils.isNullOrEmpty(s)) {
                return "null";
            }
            return s;
        }
    }
}
