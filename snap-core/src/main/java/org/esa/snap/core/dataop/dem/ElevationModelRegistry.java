/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataop.dem;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ServiceLoader;

import java.util.Set;

/**
 * An <code>ElevationModelRegistry</code> provides access to multiple different
 * elevation models as described by their {@link ElevationModelDescriptor}s.
 *
 * @author Norman Fomferra
 * @version $Revision$
 */
public class ElevationModelRegistry {

    private final ServiceRegistry<ElevationModelDescriptor> descriptors;

    private ElevationModelRegistry() {
        descriptors = ServiceRegistryManager.getInstance().getServiceRegistry(ElevationModelDescriptor.class);
        ServiceLoader.loadServices(descriptors);
    }

    public static ElevationModelRegistry getInstance() {
        return Holder.instance;
    }

    public void addDescriptor(ElevationModelDescriptor elevationModelDescriptor) {
        descriptors.addService(elevationModelDescriptor);
    }

    public void removeDescriptor(ElevationModelDescriptor elevationModelDescriptor) {
        descriptors.removeService(elevationModelDescriptor);
    }

    public ElevationModelDescriptor getDescriptor(String demName) {
        Guardian.assertNotNullOrEmpty("demName", demName);
        Set<ElevationModelDescriptor> services = descriptors.getServices();
        for (ElevationModelDescriptor descriptor : services) {
            if (demName.equalsIgnoreCase(descriptor.getName())) {
                return descriptor;
            }
        }
        return null;
    }

    public ElevationModelDescriptor[] getAllDescriptors() {
        return descriptors.getServices().toArray(new ElevationModelDescriptor[0]);
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final ElevationModelRegistry instance = new ElevationModelRegistry();
    }
}
