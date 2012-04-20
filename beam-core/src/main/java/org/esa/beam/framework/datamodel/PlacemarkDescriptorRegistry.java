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
package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.beam.BeamCoreActivator;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlacemarkDescriptorRegistry {

    private static PlacemarkDescriptorRegistry instance = new PlacemarkDescriptorRegistry();
    private ServiceRegistry<PlacemarkDescriptor> serviceRegistry;

    public PlacemarkDescriptorRegistry() {
        ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        serviceRegistry = serviceRegistryManager.getServiceRegistry(PlacemarkDescriptor.class);
        if (!BeamCoreActivator.isStarted()) {
            BeamCoreActivator.loadServices(serviceRegistry);
        }
    }

    public static PlacemarkDescriptorRegistry getInstance() {
        return instance;
    }

    public static void setInstance(PlacemarkDescriptorRegistry instance) {
        Assert.notNull(instance, "instance");
        PlacemarkDescriptorRegistry.instance = instance;
    }

    public PlacemarkDescriptor getPlacemarkDescriptor(Class<? extends PlacemarkDescriptor> clazz) {
        return getPlacemarkDescriptor(clazz.getName());
    }

    public PlacemarkDescriptor getPlacemarkDescriptor(String className) {
        return serviceRegistry.getService(className);
    }

    public Set<PlacemarkDescriptor> getPlacemarkDescriptors() {
        return serviceRegistry.getServices();
    }

    public List<PlacemarkDescriptor> getValidPlacemarkDescriptors(SimpleFeatureType featureType) {
        ArrayList<PlacemarkDescriptor> list = new ArrayList<PlacemarkDescriptor>();
        for (PlacemarkDescriptor placemarkDescriptor : getPlacemarkDescriptors()) {
            if (placemarkDescriptor.getQualification(featureType) != DecodeQualification.UNABLE) {
                list.add(placemarkDescriptor);
            }
        }
        return list;
    }

    public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType featureType) {
        ArrayList<PlacemarkDescriptor> list = new ArrayList<PlacemarkDescriptor>();
        for (PlacemarkDescriptor placemarkDescriptor : getPlacemarkDescriptors()) {
            DecodeQualification qualification = placemarkDescriptor.getQualification(featureType);
            if (qualification == DecodeQualification.INTENDED) {
                return placemarkDescriptor;
            }  else if (qualification == DecodeQualification.SUITABLE) {
                list.add(placemarkDescriptor);
            }
        }
        if (!list.isEmpty()) {
            return list.get(0);
        } else {
            return null;
        }
    }

}
