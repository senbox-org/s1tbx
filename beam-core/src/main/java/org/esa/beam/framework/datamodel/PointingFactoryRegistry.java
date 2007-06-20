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

import java.util.HashMap;
import java.util.Set;

import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.core.ServiceRegistry;
import org.esa.beam.util.Debug;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision: 1.3 $ $Date: 2007/03/22 14:11:08 $
 */
public class PointingFactoryRegistry {

    private static PointingFactoryRegistry instance;

    private final HashMap<String, PointingFactory> typeToFactoryMap = new HashMap<String, PointingFactory>();


    private PointingFactoryRegistry() {

    }

    public synchronized static PointingFactoryRegistry getInstance() {
        if (instance == null) {
            instance = new PointingFactoryRegistry();
            ServiceRegistryFactory factory = ServiceRegistryFactory.getInstance();
            ServiceRegistry<PointingFactory> pointingRegistry = factory.getServiceRegistry(PointingFactory.class);
            Set<PointingFactory> pointingFactorySet = pointingRegistry.getServices();
            Debug.trace("registering pointing factories...");
            for (PointingFactory pointingFactory : pointingFactorySet) {
                instance.addFactory(pointingFactory);
                Debug.trace("pointing factory registered: " + pointingFactory.getClass().getName());
            }
        }
        return instance;
    }

    public PointingFactory getPointingFactory(String productType) {
        return typeToFactoryMap.get(productType);
    }

    public void addFactory(PointingFactory pointingFactory) {
        String[] supportedProductTypes = pointingFactory.getSupportedProductTypes();
        for (String productType : supportedProductTypes) {
            typeToFactoryMap.put(productType, pointingFactory);
        }
    }

}
