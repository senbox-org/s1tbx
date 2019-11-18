/*
 * Copyright (C) 2019 by SkyWatch Space Applications http://www.skywatch.com
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
package org.esa.s1tbx.calibration.gpf.support;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ServiceLoader;

import java.util.Set;

/**
 * An {@code CalibratorRegistry} provides access to multiple different Calibrators.
 */
public class CalibratorRegistry {

    private final ServiceRegistry<Calibrator> calibrators;

    private CalibratorRegistry() {
        calibrators = ServiceRegistryManager.getInstance().getServiceRegistry(Calibrator.class);
        ServiceLoader.loadServices(calibrators);
    }

    public static CalibratorRegistry getInstance() {
        return Holder.instance;
    }

    public Calibrator getCalibrator(final String name) throws Exception {
        Guardian.assertNotNullOrEmpty("name", name);
        final Set<Calibrator> services = calibrators.getServices();
        for (Calibrator descriptor : services) {
            final String[] missions = descriptor.getSupportedMissions();
            for(String mission : missions) {
                if (name.equalsIgnoreCase(mission)) {
                    return descriptor.getClass().newInstance();
                }
            }
        }
        return null;
    }

    public Calibrator[] getAllCalibrators() {
        return calibrators.getServices().toArray(new Calibrator[calibrators.getServices().size()]);
    }

    // Initialization on demand holder idiom
    private static class Holder {
        private static final CalibratorRegistry instance = new CalibratorRegistry();
    }
}
