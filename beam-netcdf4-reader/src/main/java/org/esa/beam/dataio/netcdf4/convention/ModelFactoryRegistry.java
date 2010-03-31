package org.esa.beam.dataio.netcdf4.convention;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.beam.BeamCoreActivator;
import org.esa.beam.dataio.netcdf4.Nc4ReaderParameters;
import org.esa.beam.dataio.netcdf4.convention.cf.CfModelFactory;

import java.util.Set;

/**
 * User: Thomas Storm
 * Date: 29.03.2010
 * Time: 14:38:56
 */
public class ModelFactoryRegistry {

    private ServiceRegistry<AbstractModelFactory> serviceRegistry;

    private ModelFactoryRegistry() {
        ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        serviceRegistry = serviceRegistryManager.getServiceRegistry(AbstractModelFactory.class);
        if (!BeamCoreActivator.isStarted()) {
            BeamCoreActivator.loadServices(serviceRegistry);
        }
    }

    public static ModelFactoryRegistry getInstance() {
        return Holder.instance;
    }

    public AbstractModelFactory getModelFactory(Nc4ReaderParameters rp) {
        final Set<AbstractModelFactory> modelFactories = serviceRegistry.getServices();
        for (AbstractModelFactory modelFactory : modelFactories) {
            if (modelFactory.isIntendedFor(rp)) {
                return modelFactory;
            }
        }
        return serviceRegistry.getService(CfModelFactory.class.getName());
    }

    private static class Holder {

        private static ModelFactoryRegistry instance = new ModelFactoryRegistry();
    }
}
