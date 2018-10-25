/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.landcover.dataio;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ServiceLoader;

import java.util.Set;

/**
 * An {@code LandCoverModelRegistry} provides access to multiple different
 * land cover models as described by their LandCoverModelDescriptors.
 */
public class LandCoverModelRegistry {

    private final ServiceRegistry<LandCoverModelDescriptor> descriptors;

    private LandCoverModelRegistry() {
        descriptors = ServiceRegistryManager.getInstance().getServiceRegistry(LandCoverModelDescriptor.class);
        ServiceLoader.loadServices(descriptors);
    }

    public static LandCoverModelRegistry getInstance() {
        return Holder.instance;
    }

    public void addDescriptor(final LandCoverModelDescriptor modelDescriptor) {
        descriptors.addService(modelDescriptor);
    }

    public void removeDescriptor(final LandCoverModelDescriptor modelDescriptor) {
        descriptors.removeService(modelDescriptor);
    }

    public LandCoverModelDescriptor getDescriptor(final String name) {
        Guardian.assertNotNullOrEmpty("name", name);
        final Set<LandCoverModelDescriptor> services = descriptors.getServices();
        for (LandCoverModelDescriptor descriptor : services) {
            if (name.equalsIgnoreCase(descriptor.getName())) {
                return descriptor;
            }
        }
        return null;
    }

    public LandCoverModelDescriptor[] getAllDescriptors() {
        return descriptors.getServices().toArray(new LandCoverModelDescriptor[descriptors.getServices().size()]);
    }

    // Initialization on demand holder idiom
    private static class Holder {
        private static final LandCoverModelRegistry instance = new LandCoverModelRegistry();
    }
}
