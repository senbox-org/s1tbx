/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * A simple registry for {@link org.esa.beam.binning.PostProcessorDescriptor}s.
 *
 * @author MarcoZ
 * @author Norman
 */
public class PostProcessorDescriptorRegistry {

    private final Map<String, PostProcessorDescriptor> map;

    private PostProcessorDescriptorRegistry() {
        map = new HashMap<String, PostProcessorDescriptor>();
        for (PostProcessorDescriptor descriptor : ServiceLoader.load(PostProcessorDescriptor.class)) {
            map.put(descriptor.getName(), descriptor);
        }

        if (map.isEmpty()) {
            // todo - clarify why this is needed when the operator is being used in VISAT
            final ServiceRegistry<PostProcessorDescriptor> serviceRegistry = ServiceRegistryManager.getInstance().getServiceRegistry(PostProcessorDescriptor.class);
            final Set<PostProcessorDescriptor> PostProcessorDescriptors = serviceRegistry.getServices();

            for (PostProcessorDescriptor descriptor : PostProcessorDescriptors) {
                map.put(descriptor.getName(), descriptor);
            }
        }

    }

    public static PostProcessorDescriptorRegistry getInstance() {
        return Holder.instance;
    }

    public PostProcessorDescriptor getPostProcessorDescriptor(String name) {
        return map.get(name);
    }

    public PostProcessorDescriptor[] getPostProcessorDescriptors() {
        final PostProcessorDescriptor[] PostProcessorDescriptors = new PostProcessorDescriptor[map.size()];
        final Collection<PostProcessorDescriptor> values = map.values();
        int index = 0;
        for (PostProcessorDescriptor value : values) {
            PostProcessorDescriptors[index++] = value;
        }
        return PostProcessorDescriptors;
    }

    // Initialization-on-demand holder idiom
    private static class Holder {

        private static final PostProcessorDescriptorRegistry instance = new PostProcessorDescriptorRegistry();
    }
}
