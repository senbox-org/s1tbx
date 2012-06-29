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

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Simple registry for {@link StatisticsCalculatorDescriptor}s.
 *
 * @author Thomas Storm
 */
public class StatisticsCalculatorDescriptorRegistry {

    private final Map<String, StatisticsCalculatorDescriptor> map;

    private StatisticsCalculatorDescriptorRegistry() {
        map = new HashMap<String, StatisticsCalculatorDescriptor>();
        for (StatisticsCalculatorDescriptor statisticsCalculator : ServiceLoader.load(StatisticsCalculatorDescriptor.class)) {
            map.put(statisticsCalculator.getName().toLowerCase(), statisticsCalculator);
        }
    }

    public static StatisticsCalculatorDescriptorRegistry getInstance() {
        return Holder.instance;
    }

    public StatisticsCalculatorDescriptor getStatisticsCalculatorDescriptor(String name) {
        return map.get(name.toLowerCase());
    }

    public StatisticsCalculatorDescriptor[] getStatisticCalculatorDescriptors() {
        return map.values().toArray(new StatisticsCalculatorDescriptor[map.size()]);
    }

    // Initialization-on-demand holder idiom
    private static class Holder {

        private static final StatisticsCalculatorDescriptorRegistry instance = new StatisticsCalculatorDescriptorRegistry();
    }

}
