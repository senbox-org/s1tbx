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

import com.bc.ceres.core.ProgressMonitor;

import java.util.Map;

/**
 * A statistics calculator is an implementation of an algorithm that calculates statistics on a set of measurement
 * values.
 *
 * @author Thomas Storm
 */
public interface StatisticsCalculator {

    /**
     * Calculates the statistics of the given input pixel values and puts the results into a map; thus, implementors
     * may compute multiple statistical values (such as min/max).
     *
     * @param values The values on which the statistics shall be computed.
     * @param pm     A progress monitor.
     *
     * @return A map containing the statistical value mapped to its name; e.g. something like "Min" => 0.07
     */
    Map<String, Double> calculateStatistics(double[] values, ProgressMonitor pm);

}
