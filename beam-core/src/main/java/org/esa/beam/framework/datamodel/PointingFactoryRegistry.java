/*
 * $Id: PointingFactoryRegistry.java,v 1.3 2007/03/22 14:11:08 norman Exp $
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
package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;
import org.esa.beam.BeamCoreActivator;
import org.esa.beam.util.Guardian;

import java.util.Set;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class PointingFactoryRegistry {

    private static PointingFactoryRegistry instance;

    private static ServiceRegistry<PointingFactory> typeToFactoryMap;


    private PointingFactoryRegistry() {

    }

    public synchronized static PointingFactoryRegistry getInstance() {
        if (instance == null) {
            instance = new PointingFactoryRegistry();
            ServiceRegistryFactory factory = ServiceRegistryFactory.getInstance();
            typeToFactoryMap = factory.getServiceRegistry(PointingFactory.class);
            if (!BeamCoreActivator.isStarted()) {
                BeamCoreActivator.loadServices(typeToFactoryMap);
            }
        }
        return instance;
    }

    public PointingFactory getPointingFactory(String productType) {
        Guardian.assertNotNullOrEmpty("productType", productType);
        Set<PointingFactory> services = typeToFactoryMap.getServices();
        for (PointingFactory descriptor : services) {
            String[] supportedProductTypes = descriptor.getSupportedProductTypes();
            for (String supportedType : supportedProductTypes) {
                if (productType.equalsIgnoreCase(supportedType)) {
                    return descriptor;
                }
            }
        }
        return null;
    }

    public void addFactory(PointingFactory pointingFactory) {
            typeToFactoryMap.addService(pointingFactory);
    }

}
