/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * A simple registry for {@link AggregatorDescriptor}s.
 *
 * @author MarcoZ
 * @author Norman
 */
public class AggregatorDescriptorRegistry {
    private final Map<String, AggregatorDescriptor> map;

    private AggregatorDescriptorRegistry() {
        map = new HashMap<String, AggregatorDescriptor>();
        for (AggregatorDescriptor descriptor : ServiceLoader.load(AggregatorDescriptor.class)) {
            map.put(descriptor.getName(), descriptor);
        }
    }

    public static AggregatorDescriptorRegistry getInstance() {
        return Holder.instance;
    }

    public AggregatorDescriptor getAggregatorDescriptor(String name) {
        return map.get(name);
    }

    public AggregatorDescriptor[] getAggregatorDescriptors() {
        final AggregatorDescriptor[] aggregatorDescriptors = new AggregatorDescriptor[map.size()];
        final Collection<AggregatorDescriptor> values = map.values();
        int index = 0;
        for (AggregatorDescriptor value : values) {
            aggregatorDescriptors[index++] = value;
        }
        return aggregatorDescriptors;
    }

    // Initialization-on-demand holder idiom
    private static class Holder {
        private static final AggregatorDescriptorRegistry instance = new AggregatorDescriptorRegistry();
    }
}
