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

import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.binning.AggregatorDescriptor;

/**
 * @author thomas
 */
class TargetVariableSpec {

    String targetPrefix;
    Source source;
    String aggregationString;
    AggregatorDescriptor aggregatorDescriptor;
    PropertyContainer aggregatorProperties;

    static class Source {

        static final int BAND_SOURCE_TYPE = 0;
        static final int EXPRESSION_SOURCE_TYPE = 1;

        String bandName;
        String expression;
        int type;
    }
}
