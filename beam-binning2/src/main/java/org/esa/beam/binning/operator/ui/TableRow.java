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

import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.util.StringUtils;

/**
 * Simple configuration class containing a number of public final fields.
 *
 * @author Thomas Storm
 */
class TableRow {

    final String name;
    final String expression;
    final AggregatorDescriptor aggregator;
    final Double weight;
    final Integer percentile;

    TableRow(String name, String expression, AggregatorDescriptor aggregator, Double weight, Integer percentile) {
        this.name = name.replace("<", "").replace(">", "");
        this.expression = StringUtils.isNullOrEmpty(expression) ? null : expression;
        this.aggregator = aggregator;
        this.weight = weight;
        this.percentile = percentile;
    }

    @Override
    public String toString() {
        return "TableRow{" +
               "aggregator=" + aggregator +
               ", name='" + name + '\'' +
               ", expression='" + expression + '\'' +
               ", weight=" + weight +
               ", percentile=" + percentile +
               '}';
    }
}
