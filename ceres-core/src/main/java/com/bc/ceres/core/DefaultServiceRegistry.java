package com.bc.ceres.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @since 0.6
 */
public class DefaultServiceRegistry<T> implements ServiceRegistry<T> {

    private final Class<T> serviceType;
    private HashSet<T> services = new HashSet<T>(10);

    private ArrayList<ServiceRegistryListener<T>> listeners = new ArrayList<ServiceRegistryListener<T>>(3);

    public DefaultServiceRegistry(Class<T> serviceType) {
        Assert.notNull(serviceType, "serviceType");
        this.serviceType = serviceType;
    }

    public Class<T> getServiceType() {
        return serviceType;
    }

    public Set<T> getServices() {
        return (Set<T>) services.clone();
    }

    public T getService(String className) {
        for (T service : services) {
            if (service.getClass().getName().equals(className)) {
                return service;
            }
        }
        return null;
    }

    public boolean addService(T service) {
        Assert.notNull(service, "service");
        if (!services.contains(service)) {
            services.add(service);
            for (ServiceRegistryListener<T> listener : listeners) {
                listener.serviceAdded(this, service);
            }
            return true;
        }
        return false;
    }

    public boolean removeService(T service) {
        Assert.notNull(service, "service");
        if (services.remove(service)) {
            for (ServiceRegistryListener<T> listener : listeners) {
                listener.serviceRemoved(this, service);
            }
            return true;
        }
        return false;
    }

    public List<ServiceRegistryListener<T>> getListeners() {
        return (List<ServiceRegistryListener<T>>) listeners.clone();
    }

    public void addListener(ServiceRegistryListener<T> listener) {
        Assert.notNull(listener, "listener");
        listeners.add(listener);
    }

    public void removeListener(ServiceRegistryListener<T> listener) {
        Assert.notNull(listener, "listener");
        listeners.remove(listener);
    }
}
