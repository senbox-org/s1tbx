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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.util.ServiceLoader;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class PlacemarkDescriptorRegistry {

    public final static String PROPERTY_NAME_PLACEMARK_DESCRIPTOR = AbstractPlacemarkDescriptor.PROPERTY_NAME_PLACEMARK_DESCRIPTOR;

    private ServiceRegistry<PlacemarkDescriptor> serviceRegistry;

    public PlacemarkDescriptorRegistry(ServiceRegistry<PlacemarkDescriptor> serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    private PlacemarkDescriptorRegistry() {
        ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        serviceRegistry = serviceRegistryManager.getServiceRegistry(PlacemarkDescriptor.class);
        ServiceLoader.loadServices(serviceRegistry);
    }

    public static PlacemarkDescriptorRegistry getInstance() {
        return Holder.instance;
    }

    public static void setInstance(PlacemarkDescriptorRegistry instance) {
        Assert.notNull(instance, "instance");
        Holder.instance = instance;
    }

    /**
     * Gives a placemark descriptors from the service registry which is compatible with the given class which
     * extends {@link PlacemarkDescriptor}.
     *
     * @param clazz  The class.
     * @return  the placemark descriptor
     */
    public PlacemarkDescriptor getPlacemarkDescriptor(Class<? extends PlacemarkDescriptor> clazz) {
        return getPlacemarkDescriptor(clazz.getName());
    }

    /**
     * Gives a placemark descriptor from the service registry which is compatible with the given class name.
     *
     * @param className  The class name.
     * @return  the placemark descriptor
     */
    public PlacemarkDescriptor getPlacemarkDescriptor(String className) {
        return serviceRegistry.getService(className);
    }

    /**
     * Gives all placemark descriptors from the service registry.
     *
     * @return  the placemark descriptors
     */
    public Set<PlacemarkDescriptor> getPlacemarkDescriptors() {
        return serviceRegistry.getServices();
    }

    /**
     * Returns an ordered list of placemark descriptors that are compatible with the given feature type.
     * The list is sorted by by the level of compatibility (see {@link DecodeQualification}). A feature type may have
     * given the class name of an appropriate placemark descriptor in its "user data", the key for that descriptor name is
     * given by {@link AbstractPlacemarkDescriptor#PROPERTY_NAME_PLACEMARK_DESCRIPTOR}.
     *
     * @param featureType The feature type.
     * @return An ordered list of descriptors, which may be empty.
     */
    public List<PlacemarkDescriptor> getPlacemarkDescriptors(final SimpleFeatureType featureType) {
        ArrayList<PlacemarkDescriptor> list = new ArrayList<PlacemarkDescriptor>();
        for (PlacemarkDescriptor placemarkDescriptor : getPlacemarkDescriptors()) {
            DecodeQualification qualification = placemarkDescriptor.getCompatibilityFor(featureType);
            if (qualification != DecodeQualification.UNABLE) {
                if (qualification == DecodeQualification.INTENDED) {
                    list.add(placemarkDescriptor);
                } else {
                    list.add(placemarkDescriptor);
                }
            }
        }
        Collections.sort(list, new Comparator<PlacemarkDescriptor>() {
            @Override
            public int compare(PlacemarkDescriptor o1, PlacemarkDescriptor o2) {
                boolean isO1Intended = o1.getCompatibilityFor(featureType) == DecodeQualification.INTENDED;
                boolean isO2Intended = o2.getCompatibilityFor(featureType) == DecodeQualification.INTENDED;
                if (isO1Intended && !isO2Intended) {
                    return -1;
                } else if (!isO1Intended && isO2Intended) {
                    return 1;
                } else if (isO1Intended && isO2Intended) {
                    if (hasClassProperty(o1) && !hasClassProperty(o2)) {
                        return -1;
                    } else if (!hasClassProperty(o1) && hasClassProperty(o2)) {
                        return 1;
                    }
                }
                return 0;
            }
        });
        return list;
    }

    /**
     * Returns the 'best qualified' placemark descriptors which is compatible with the given feature type.
     *
     * @param featureType The feature type.
     * @return the placemark descriptor
     */
    public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType featureType) {
        PlacemarkDescriptor suitablePlacemarkDescriptor = null;
        PlacemarkDescriptor intendedPlacemarkDescriptor = null;
        for (PlacemarkDescriptor placemarkDescriptor : getPlacemarkDescriptors()) {
            DecodeQualification qualification = placemarkDescriptor.getCompatibilityFor(featureType);
            if (qualification == DecodeQualification.INTENDED) {
                if (hasClassProperty(placemarkDescriptor)) {
                    return placemarkDescriptor;
                } else {
                    intendedPlacemarkDescriptor = placemarkDescriptor;
                }
            } else if (qualification == DecodeQualification.SUITABLE) {
                suitablePlacemarkDescriptor = placemarkDescriptor;
            }
        }
        if (intendedPlacemarkDescriptor != null) {
            return intendedPlacemarkDescriptor;
        }
        return suitablePlacemarkDescriptor;
    }

    private static boolean hasClassProperty(PlacemarkDescriptor placemarkDescriptor) {
        return placemarkDescriptor.getBaseFeatureType().getUserData().containsKey(PROPERTY_NAME_PLACEMARK_DESCRIPTOR);
    }

    private static class Holder {

        private static PlacemarkDescriptorRegistry instance = new PlacemarkDescriptorRegistry();
    }

}
