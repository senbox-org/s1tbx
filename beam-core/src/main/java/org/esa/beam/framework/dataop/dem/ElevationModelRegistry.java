/*
 * $Id: ElevationModelRegistry.java,v 1.5 2007/03/22 16:41:43 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.dem;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;
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

    private static ElevationModelRegistry instance;
    private final ServiceRegistry<ElevationModelDescriptor> descriptors;

    private ElevationModelRegistry() {
        descriptors = ServiceRegistryFactory.getInstance().getServiceRegistry(ElevationModelDescriptor.class);
        if (!BeamCoreActivator.isStarted()) {
            BeamCoreActivator.loadServices(descriptors);
        }
    }

    public synchronized static ElevationModelRegistry getInstance() {
        if (instance == null) {
            instance = new ElevationModelRegistry();
        }
        return instance;
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
}
