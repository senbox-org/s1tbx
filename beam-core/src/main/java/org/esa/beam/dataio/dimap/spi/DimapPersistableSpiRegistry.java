/*
 * $Id: DimapPersistableSpiRegistry.java,v 1.1 2007/03/22 16:56:59 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.dimap.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.esa.beam.util.Debug;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.BeamCoreActivator;
import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.core.ServiceRegistry;


/**
 * A registry for dimap service provider instances.
 *
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 *
 */

public final class DimapPersistableSpiRegistry {

    private ServiceRegistry<DimapPersistableSpi> providers;
    private static DimapPersistableSpiRegistry instance;

    private DimapPersistableSpiRegistry() {
        providers = ServiceRegistryFactory.getInstance().getServiceRegistry(DimapPersistableSpi.class);
        if (!BeamCoreActivator.isStarted()) {
            BeamCoreActivator.loadServices(providers);
        }
    }

    /**
     * Gets the singelton instance of this class.
     *
     * @return the instance
     */
    public static DimapPersistableSpiRegistry getInstance(){
        if(instance == null) {
            instance = new DimapPersistableSpiRegistry();
            ServiceRegistryFactory factory = ServiceRegistryFactory.getInstance();
            ServiceRegistry<DimapPersistableSpi> persistableRegistry = factory.getServiceRegistry(DimapPersistableSpi.class);
            Set<DimapPersistableSpi> persistableSpis = persistableRegistry.getServices();
            Debug.trace("registering dimap persistable service provider...");
            for (DimapPersistableSpi spi : persistableSpis) {
                instance.addPersistableSpi(spi);
                Debug.trace("dimap persistable service provider registered: " + spi.getClass().getName());
            }
        }
        return instance;
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
}
