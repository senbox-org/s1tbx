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

package org.esa.snap.binning;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.util.ServiceLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple registry for {@link TypedDescriptor}s.
 *
 * @author MarcoZ
 * @author Norman
 */
public class TypedDescriptorsRegistry {

    private final Map<Class, SpecificRegistry> map;

    protected TypedDescriptorsRegistry() {
        map = new HashMap<>();
    }

    private synchronized <TD extends TypedDescriptor> SpecificRegistry getSpecificRegistry(Class<TD> klass) {
        SpecificRegistry specificRegistry = map.get(klass);
        if (specificRegistry == null) {
            specificRegistry = new SpecificRegistry(klass);
            map.put(klass, specificRegistry);
        }
        return specificRegistry;
    }

    public <TD extends TypedDescriptor> List<TD> getDescriptors(Class<TD> klass) {
        SpecificRegistry<TD> specificRegistry = getSpecificRegistry(klass);
        return specificRegistry.getDescriptors();
    }

    public <TD extends TypedDescriptor> TD getDescriptor(Class<TD> klass, String name) {
        SpecificRegistry<TD> specificRegistry = getSpecificRegistry(klass);
        return specificRegistry.getDescriptor(name);
    }

    public static TypedDescriptorsRegistry getInstance() {
        return Holder.instance;
    }

    // Initialization-on-demand holder idiom
    private static class Holder {
        private static final TypedDescriptorsRegistry instance = new TypedDescriptorsRegistry();
    }

    private class SpecificRegistry<TD extends TypedDescriptor> {

        private final Map<String, TD> nameMap;

        SpecificRegistry(Class<TD> klass) {
            ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
            ServiceRegistry<TD> serviceRegistry = serviceRegistryManager.getServiceRegistry(klass);
            ServiceLoader.loadServices(serviceRegistry);

            nameMap = createNameMap(serviceRegistry);
        }

        private Map<String, TD> createNameMap(ServiceRegistry<TD> serviceRegistry) {
            Map<String, TD> map = new HashMap<>();
            final Set<TD> descriptors = serviceRegistry.getServices();

            for (TD descriptor : descriptors) {
                map.put(descriptor.getName(), descriptor);
            }
            return map;
        }

        TD getDescriptor(String name) {
            return nameMap.get(name);
        }

        List<TD> getDescriptors() {
            return Collections.unmodifiableList(new ArrayList<>(nameMap.values()));
        }

    }

}
