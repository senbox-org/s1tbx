/*
 * $Id: ElevationModelRegistry.java,v 1.5 2007/03/22 16:41:43 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.dem;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;

import org.esa.beam.BeamCoreActivator;
import org.esa.beam.util.Guardian;

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
        if (!BeamCoreActivator.isStarted()) {
            BeamCoreActivator.loadServices(descriptors);
        }
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
