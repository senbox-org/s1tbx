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
package org.esa.snap.core.dataop.maptransf;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ServiceLoader;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A registry for map projection instances.
 * 
 * @deprecated since BEAM 4.7, use geotools and {@link CrsGeoCoding} instead.
 */
@Deprecated
public class MapProjectionRegistry {

    private static final ServiceRegistry<MapTransformDescriptor> descriptors;
    private static final List<MapProjection> projectionList;

    static {
        projectionList = new LinkedList<>();
        ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        descriptors = serviceRegistryManager.getServiceRegistry(MapTransformDescriptor.class);
        ServiceLoader.loadServices(descriptors);
        Set<MapTransformDescriptor> services = descriptors.getServices();
        for (MapTransformDescriptor descriptor : services) {
            descriptor.registerProjections();               
        }
    }

    /**
     * Registers a shared map-projection instance in this registry.
     *
     * @param projection the map-projection to be added
     */
    public static void registerProjection(MapProjection projection) {
        if (projection != null && !projectionList.contains(projection)) {
            projectionList.add(projection);
        }
    }

    /**
     * De-registers a shared map-projection instance in this registry.
     *
     * @param projection the map-projection to be removed
     */
    public static void deregisterProjection(MapProjection projection) {
        if (projection != null && !projectionList.contains(projection)) {
            projectionList.remove(projection);
        }
    }

    /**
     * Gets all registered map-projections.
     *
     * @return an array of all registered map-projections, never <code>null</code>
     */
    public static MapProjection[] getProjections() {
        return projectionList.toArray(new MapProjection[projectionList.size()]);
    }

    /**
     * Gets the map-projection instance with the given name.
     *
     * @param name a map-projection name, must not be <code>null</code>
     *
     * @return the map-projection instance or <code>null</code>
     */
    public static MapProjection getProjection(String name) {
        for (MapProjection projection : projectionList) {
            if (name.equalsIgnoreCase(projection.getName())) {
                return projection;
            }
        }
        return null;
    }

    /**
     * Registers a new map transformation type. After the descriptor has been registered, its {@link
     * MapTransformDescriptor#registerProjections} method is called.
     *
     * @param descriptor the new map transformation descriptor
     */
    public static void registerDescriptor(MapTransformDescriptor descriptor) {
        descriptors.addService(descriptor);
        descriptor.registerProjections();
    }

    /**
     * Gets all registered map transformation descriptors.
     *
     * @return an array of all registered descriptors, never <code>null</code>
     */
    public static MapTransformDescriptor[] getDescriptors() {
        return descriptors.getServices().toArray(new MapTransformDescriptor[0]);
    }

    /**
     * Gets the descriptor for the given type ID.
     *
     * @param typeID the map transform type ID, must not be null
     *
     * @return the descriptor, or <code>null</code> if the type ID is unknown
     */
    public static MapTransformDescriptor getDescriptor(String typeID) {
        Guardian.assertNotNullOrEmpty("typeID", typeID);
        Set<MapTransformDescriptor> services = descriptors.getServices();
        for (MapTransformDescriptor descriptor : services) {
            if (typeID.equalsIgnoreCase(descriptor.getTypeID())) {
                return descriptor;
            }
        }
        return null;
    }
}
