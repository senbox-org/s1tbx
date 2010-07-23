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
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.Guardian;
import ucar.nc2.NetcdfFile;

import java.util.Set;

/**
 * A registry for {@link ProfileSpi}s.
 *
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
            DecodeQualification qualification = DecodeQualification.UNABLE;
            try {
                qualification = profileSpi.getDecodeQualification(netcdfFile);
            }catch (Exception ignore) {
            }
            if (qualification == DecodeQualification.SUITABLE) {
                bestQualification = qualification;
            } else if (qualification == DecodeQualification.INTENDED) {
                return qualification;
            }
        }
        return bestQualification;
    }

    public ProfileSpi getProfileFactory(String profileName) {
        Guardian.assertNotNullOrEmpty("profileName", profileName);
        Set<ProfileSpi> services = serviceRegistry.getServices();
        for (ProfileSpi profileSpi : services) {
            if (profileName.equalsIgnoreCase(profileSpi.getClass().getName())) {
                return profileSpi;
            }
        }
        return null;
    }

    private static class Holder {

        private static ProfileSpiRegistry instance = new ProfileSpiRegistry();
    }
}
