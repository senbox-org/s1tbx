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

package org.esa.beam.statistics.calculators;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;

import java.util.Map;

/**
 * Implementation of {@link StatisticsCalculator} that computes a simple percentile.
 *
 * @author Thomas Storm
 */
public class PercentileStatisticsCalculator implements StatisticsCalculator {

    final int percentile;

    public PercentileStatisticsCalculator(int percentile) {
        this.percentile = percentile;
    }

    @Override
    public Map<String, Double> calculateStatistics(double[] values, ProgressMonitor pm) {
        return null;
    }

    public static class Descriptor implements StatisticsCalculatorDescriptor {

        @Override
        public String getName() {
            return "PERCENTILE";
        }

        @Override
        public StatisticsCalculator createStatisticsCalculator(PropertySet propertySet) {
            final int percentile = propertySet.getProperty("percentile").<Integer>getValue();
            return new PercentileStatisticsCalculator(percentile);
        }
    }

}
