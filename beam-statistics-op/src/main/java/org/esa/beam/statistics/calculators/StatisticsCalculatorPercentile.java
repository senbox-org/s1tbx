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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link StatisticsCalculator} that computes a simple percentile.
 *
 * @author Thomas Storm
 */
public class StatisticsCalculatorPercentile implements StatisticsCalculator {

    final int percentile;

    public StatisticsCalculatorPercentile(int percentile) {
        this.percentile = percentile;
    }

    @Override
    public Map<String, Double> calculateStatistics(double[] values, ProgressMonitor pm) {
        final HashMap<String, Double> result = new HashMap<String, Double>();
        Arrays.sort(values);
        result.put("p" + percentile, computePercentile(values));
        return result;
    }

    /**
     * Computes the p-th percentile of an array of measurements following
     * the "Engineering Statistics Handbook: Percentile". NIST.
     * http://www.itl.nist.gov/div898/handbook/prc/section2/prc252.htm.
     * Retrieved 2011-03-16.
     *
     * @param measurements Sorted array of measurements.
     * @return The  p-th percentile.
     *
     * todo : move this and its origin (org.esa.beam.binning.aggregators.AggregatorPercentile#computePercentile())
     * to some utility class
     */
    public double computePercentile(double[] measurements) {
        int N = measurements.length;
        double n = (percentile / 100.0F) * (N + 1);
        int k = (int) Math.floor(n);
        double d = n - k;
        double yp;
        if (k == 0) {
            yp = measurements[0];
        } else if (k >= N) {
            yp = measurements[N - 1];
        } else {
            yp = measurements[k - 1] + d * (measurements[k] - measurements[k - 1]);
        }
        return yp;
    }

    public static class Descriptor implements StatisticsCalculatorDescriptor {

        @Override
        public String getName() {
            return "percentile";
        }

        @Override
        public StatisticsCalculator createStatisticsCalculator(PropertySet propertySet) {
            final int percentile;
            final Property percentileProperty = propertySet.getProperty(getName());
            if (percentileProperty != null) {
                percentile = percentileProperty.<Integer>getValue();
            } else {
                throw new IllegalArgumentException("Missing property '" + getName() + "'");
            }
            return new StatisticsCalculatorPercentile(percentile);
        }
    }

}
