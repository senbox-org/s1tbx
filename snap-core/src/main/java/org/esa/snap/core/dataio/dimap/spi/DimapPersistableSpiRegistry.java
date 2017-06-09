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
package org.esa.snap.core.dataio.dimap.spi;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.util.ServiceLoader;

import java.util.Iterator;


/**
 * A registry for dimap service provider instances.
 *
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i>
 *
 */

public final class DimapPersistableSpiRegistry {

    private ServiceRegistry<DimapPersistableSpi> providers;

    private DimapPersistableSpiRegistry() {
        providers = ServiceRegistryManager.getInstance().getServiceRegistry(DimapPersistableSpi.class);
        ServiceLoader.loadServices(providers);
    }

    /**
     * Gets the singelton instance of this class.
     *
     * @return the instance
     */
    public static DimapPersistableSpiRegistry getInstance(){
        return Holder.instance;
    }

    public void addPersistableSpi(DimapPersistableSpi spi) {
        providers.addService(spi);
    }

    public Iterator<DimapPersistableSpi> getPersistableSpis() {
        return providers.getServices().iterator();
    }

    public boolean isRegistered(DimapPersistableSpi spi) {
        return providers.getService(spi.getClass().getName()) != null;
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final DimapPersistableSpiRegistry instance = new DimapPersistableSpiRegistry();
    }
}
