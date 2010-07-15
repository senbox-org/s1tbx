package org.esa.beam.dataio.netcdf.metadata;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.beam.BeamCoreActivator;
import org.esa.beam.framework.dataio.DecodeQualification;
import ucar.nc2.NetcdfFile;

import java.util.Set;

/**
 * A registry for {@link ProfileSpi}s.
 * @author Thomas Storm
 */
public class ProfileSpiRegistry {

    private ServiceRegistry<ProfileSpi> serviceRegistry;

    private ProfileSpiRegistry() {
        ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        serviceRegistry = serviceRegistryManager.getServiceRegistry(ProfileSpi.class);
        if (!BeamCoreActivator.isStarted()) {
            BeamCoreActivator.loadServices(serviceRegistry);
        }
    }

    public static ProfileSpiRegistry getInstance() {
        return Holder.instance;
    }

    public ProfileSpi getProfileFactory(NetcdfFile netcdfFile) {
        final Set<ProfileSpi> profileSpis = serviceRegistry.getServices();
        ProfileSpi selectedSpi = null;
        for (ProfileSpi profileSpi : profileSpis) {
            DecodeQualification qualification = profileSpi.getDecodeQualification(netcdfFile);
            if (qualification == DecodeQualification.SUITABLE) {
                selectedSpi = profileSpi;
            } else if (qualification == DecodeQualification.INTENDED) {
                return profileSpi;
            }
        }
        return selectedSpi;
    }

    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        final Set<ProfileSpi> profileSpis = serviceRegistry.getServices();
        DecodeQualification bestQualification = DecodeQualification.UNABLE;
        for (ProfileSpi profileSpi : profileSpis) {
            DecodeQualification qualification = profileSpi.getDecodeQualification(netcdfFile);
            if (qualification == DecodeQualification.SUITABLE) {
                bestQualification = qualification;
            } else if (qualification == DecodeQualification.INTENDED) {
                return qualification;
            }
        }
        return bestQualification;
    }

    private static class Holder {

        private static ProfileSpiRegistry instance = new ProfileSpiRegistry();
    }
}
