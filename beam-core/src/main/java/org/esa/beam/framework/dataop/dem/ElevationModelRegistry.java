/*
 * $Id: ElevationModelRegistry.java,v 1.5 2007/03/22 16:41:43 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.dem;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;
import org.esa.beam.util.Debug;

import java.util.HashMap;
import java.util.Set;

/**
 * An <code>ElevationModelRegistry</code> provides access to multiple different
 * elevation models as described by their {@link ElevationModelDescriptor}s.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.5 $
 */
public class ElevationModelRegistry {

    private static ElevationModelRegistry instance;
    private final HashMap<String, ElevationModelDescriptor> map;

    private ElevationModelRegistry() {
        map = new HashMap<String, ElevationModelDescriptor>(3);
    }

    public synchronized static ElevationModelRegistry getInstance() {
        if (instance == null) {
            instance = new ElevationModelRegistry();

            ServiceRegistryFactory factory = ServiceRegistryFactory.getInstance();
            ServiceRegistry<ElevationModelDescriptor> demRegistry = factory.getServiceRegistry(ElevationModelDescriptor.class);
            Set<ElevationModelDescriptor> demDescriptorSet = demRegistry.getServices();
            Debug.trace("registering elevation model descriptors...");
            for (ElevationModelDescriptor descriptor : demDescriptorSet) {
                instance.addDescriptor(descriptor);
                Debug.trace("elevation model descriptor registered: " + descriptor.getClass().getName());
            }
        }
        return instance;
    }

    public void addDescriptor(ElevationModelDescriptor elevationModelDescriptor) {
        map.put(elevationModelDescriptor.getName(), elevationModelDescriptor);
    }

    public void removeDescriptor(ElevationModelDescriptor elevationModelDescriptor) {
        map.remove(elevationModelDescriptor.getName());
    }

    public ElevationModelDescriptor getDescriptor(String demName) {
        return map.get(demName);
    }

    public ElevationModelDescriptor[] getAllDescriptors() {
        return map.values().toArray(new ElevationModelDescriptor[0]);
    }
}
