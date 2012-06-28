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

/**
 * A descriptor interface for {@link StatisticsCalculator} classes.
 *
 * @author Thomas Storm
 */
public interface StatisticsCalculatorDescriptor {

    /**
     * @return the corresponding {@link StatisticsCalculator}'s name.
     */
    String getName();

    /**
     * Creates an instance of the {@link StatisticsCalculator} corresponding to this descriptor.
     *
     * @param propertySet A set of properties the resulting {@link StatisticsCalculator} shall have.
     *
     * @return An instance of {@link StatisticsCalculator}.
     */
    StatisticsCalculator createStatisticsCalculator(PropertySet propertySet);

}
