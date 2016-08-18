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

package com.bc.ceres.core;

import java.util.HashMap;

/**
 * A manager for service registries.
 *
 * @since 0.10
 */
public class ServiceRegistryManager {

    private static ServiceRegistryManager instance = new ServiceRegistryManager();
    private final HashMap<Class, ServiceRegistry> serviceRegistries;

    public ServiceRegistryManager() {
        serviceRegistries = new HashMap<>(10);
    }

    public static ServiceRegistryManager getInstance() {
        return instance;
    }

    public static void setInstance(ServiceRegistryManager instance) {
        Assert.notNull(instance, "instance");
        ServiceRegistryManager.instance = instance;
    }

    public <T> ServiceRegistry<T> getServiceRegistry(Class<T> serviceType) {
        Assert.notNull(serviceType, "serviceType");
        ServiceRegistry<T> serviceRegistry = serviceRegistries.get(serviceType);
        if (serviceRegistry == null) {
            serviceRegistry = createServiceRegistry(serviceType);
            setServiceRegistry(serviceType, serviceRegistry);
        }
        return serviceRegistry;
    }

    public <T> void setServiceRegistry(Class<T> serviceType, ServiceRegistry<T> serviceRegistry) {
        serviceRegistries.put(serviceType, serviceRegistry);
    }

    protected <T> ServiceRegistry<T> createServiceRegistry(Class<T> serviceType) {
        Assert.notNull(serviceType, "serviceType");
        return new DefaultServiceRegistry<>(serviceType);
    }
}
