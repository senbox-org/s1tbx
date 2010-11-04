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

package org.esa.beam.dataio.netcdf.metadata;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.beam.BeamCoreActivator;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfProfileSpi;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.Guardian;
import ucar.nc2.NetcdfFile;

import java.util.Set;

/**
 * A registry for {@link ProfileSpi}s.
 *
 * @author Thomas Storm
 * @deprecated no replacement
 */
@Deprecated
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

    /**
     * Returns the  {@link ProfileSpi} that is best suitable for reading the given netCDf file.
     * <p/>
     * The {@link CfProfileSpi} is always checked as the last profile.
     * This is to ensure that this very generic profile does not take precedence over other profiles.
     *
     * @return The best matching profile, or <code>null</code>, if no profile is able to read the given netCDF file.
     */
    public ProfileSpi getProfileFactory(NetcdfFile netcdfFile) {
        final Set<ProfileSpi> profileSpis = serviceRegistry.getServices();
        ProfileSpi cfProfileSpi = serviceRegistry.getService(CfProfileSpi.class.getName());
        profileSpis.remove(cfProfileSpi);

        ProfileSpi bestProfileSpi = null;
        for (ProfileSpi profileSpi : profileSpis) {
            DecodeQualification qualification = profileSpi.getDecodeQualification(netcdfFile);
            if (qualification == DecodeQualification.SUITABLE) {
                bestProfileSpi = profileSpi;
            } else if (qualification == DecodeQualification.INTENDED) {
                return profileSpi;
            }
        }
        if (bestProfileSpi == null) {
            // try CfProfile as last option to prevent it from overruling other profiles.
            DecodeQualification decodeQualification = cfProfileSpi.getDecodeQualification(netcdfFile);
            if (decodeQualification != DecodeQualification.UNABLE) {
                bestProfileSpi = cfProfileSpi;
            }
        }
        return bestProfileSpi;
    }

    /**
     * Returns the best {@link DecodeQualification} that all registered profiles can provide for the given netCDf file.
     * <p/>
     * The {@link CfProfileSpi} checked as the last profile, if no other is able to read the file.
     * This is to ensure that this very generic profile does not take precedence over other profiles.
     *
     * @return The best decode qualification..
     */
    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        final Set<ProfileSpi> profileSpis = serviceRegistry.getServices();
        ProfileSpi cfProfileSpi = serviceRegistry.getService(CfProfileSpi.class.getName());
        profileSpis.remove(cfProfileSpi);

        DecodeQualification bestQualification = DecodeQualification.UNABLE;
        for (ProfileSpi profileSpi : profileSpis) {
            DecodeQualification qualification = DecodeQualification.UNABLE;
            try {
                qualification = profileSpi.getDecodeQualification(netcdfFile);
            } catch (Exception ignore) {
            }
            if (qualification == DecodeQualification.SUITABLE) {
                bestQualification = qualification;
            } else if (qualification == DecodeQualification.INTENDED) {
                return qualification;
            }
        }
        if (bestQualification == DecodeQualification.UNABLE) {
            bestQualification = cfProfileSpi.getDecodeQualification(netcdfFile);
        }
        return bestQualification;
    }

    public ProfileSpi getProfileFactory(String profileName) {
        Guardian.assertNotNullOrEmpty("profileName", profileName);
        return serviceRegistry.getService(profileName);
    }

    private static class Holder {

        private static ProfileSpiRegistry instance = new ProfileSpiRegistry();
    }
}
