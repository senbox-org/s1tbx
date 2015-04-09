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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@inheritDoc}
 */
public class DefaultServiceRegistry<T> implements ServiceRegistry<T> {

    private final Class<T> serviceType;
    private final HashMap<String, T> services;
    private final ArrayList<ServiceRegistryListener<T>> listeners;

    public DefaultServiceRegistry(Class<T> serviceType) {
        Assert.notNull(serviceType, "serviceType");
        this.serviceType = serviceType;
        this.services = new HashMap<>(10);
        this.listeners = new ArrayList<>(3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getServiceType() {
        return serviceType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<T> getServices() {
        return new HashSet<>(services.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getService(String className) {
        return services.get(className);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addService(T service) {
        Assert.notNull(service, "service");
        final T existingService = services.put(service.getClass().getName(), service);
        if (existingService != null && existingService.getClass().equals(service.getClass())) {
            return false;
        }
        for (ServiceRegistryListener<T> listener : listeners) {
            listener.serviceAdded(this, service);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeService(T service) {
        Assert.notNull(service, "service");
        final T existingService = services.remove(service.getClass().getName());
        if (existingService != service) {
            return false;
        }
        for (ServiceRegistryListener<T> listener : listeners) {
            listener.serviceRemoved(this, service);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ServiceRegistryListener<T>> getListeners() {
        return (List<ServiceRegistryListener<T>>) listeners.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(ServiceRegistryListener<T> listener) {
        Assert.notNull(listener, "listener");
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(ServiceRegistryListener<T> listener) {
        Assert.notNull(listener, "listener");
        listeners.remove(listener);
    }
}
