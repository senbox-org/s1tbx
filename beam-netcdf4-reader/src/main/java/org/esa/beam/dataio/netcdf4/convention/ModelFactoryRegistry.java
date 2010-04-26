package org.esa.beam.dataio.netcdf4.convention;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.beam.BeamCoreActivator;
import org.esa.beam.framework.dataio.DecodeQualification;
import ucar.nc2.NetcdfFile;

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

    public AbstractModelFactory getModelFactory(NetcdfFile netcdfFile) {
        final Set<AbstractModelFactory> modelFactories = serviceRegistry.getServices();
        AbstractModelFactory selectedFactory = null;
        for (AbstractModelFactory modelFactory : modelFactories) {
            DecodeQualification qualification = modelFactory.getDecodeQualification(netcdfFile);
            if (qualification == DecodeQualification.SUITABLE) {
                selectedFactory = modelFactory;
            } else if (qualification == DecodeQualification.INTENDED) {
                return modelFactory;
            }
        }
        return selectedFactory;
    }

    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        final Set<AbstractModelFactory> modelFactories = serviceRegistry.getServices();
        DecodeQualification bestQualification = DecodeQualification.UNABLE;
        for (AbstractModelFactory modelFactory : modelFactories) {
            DecodeQualification qualification = modelFactory.getDecodeQualification(netcdfFile);
            if (qualification == DecodeQualification.SUITABLE) {
                bestQualification = qualification;
            } else if (qualification == DecodeQualification.INTENDED) {
                return qualification;
            }
        }
        return bestQualification;
    }

    private static class Holder {

        private static ModelFactoryRegistry instance = new ModelFactoryRegistry();
    }
}
