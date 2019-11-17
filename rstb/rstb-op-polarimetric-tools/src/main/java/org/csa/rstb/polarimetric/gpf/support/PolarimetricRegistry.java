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
package org.csa.rstb.polarimetric.gpf.support;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.csa.rstb.polarimetric.gpf.decompositions.Decomposition;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ServiceLoader;

import java.util.Set;

/**
 * An {@code PolarimetricRegistry} provides access to multiple different polarimetric processors.
 */
public class PolarimetricRegistry {

    private final ServiceRegistry<Decomposition> decompositions;
    private final ServiceRegistry<PolarimetricSpeckleFilter> speckleFilters;

    private PolarimetricRegistry() {
        decompositions = ServiceRegistryManager.getInstance().getServiceRegistry(Decomposition.class);
        ServiceLoader.loadServices(decompositions);

        speckleFilters = ServiceRegistryManager.getInstance().getServiceRegistry(PolarimetricSpeckleFilter.class);
        ServiceLoader.loadServices(speckleFilters);
    }

    public static PolarimetricRegistry getInstance() {
        return Holder.instance;
    }

    public Decomposition getDecomposition(final String name) {
        Guardian.assertNotNullOrEmpty("name", name);
        final Set<Decomposition> services = decompositions.getServices();
        for (Decomposition descriptor : services) {

        }
        return null;
    }

    public Decomposition[] getAllDecompositions() {
        return decompositions.getServices().toArray(new Decomposition[decompositions.getServices().size()]);
    }

    public PolarimetricSpeckleFilter[] getAllSpeckleFilters() {
        return speckleFilters.getServices().toArray(new PolarimetricSpeckleFilter[speckleFilters.getServices().size()]);
    }

    // Initialization on demand holder idiom
    private static class Holder {
        private static final PolarimetricRegistry instance = new PolarimetricRegistry();
    }
}
