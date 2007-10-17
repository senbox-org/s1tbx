package com.bc.ceres.core;

import java.util.*;

/**
 * {@inheritDoc}
 */
public class DefaultServiceRegistry<T> implements ServiceRegistry<T> {

    private final Class<T> serviceType;
    private HashMap<String, T> services = new HashMap<String, T>(10);

    private ArrayList<ServiceRegistryListener<T>> listeners = new ArrayList<ServiceRegistryListener<T>>(3);

    public DefaultServiceRegistry(Class<T> serviceType) {
        Assert.notNull(serviceType, "serviceType");
        this.serviceType = serviceType;
    }

    /**
     * {@inheritDoc}
     */
    public Class<T> getServiceType() {
        return serviceType;
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> getServices() {
        return new HashSet<T>(services.values());
    }

    /**
     * {@inheritDoc}
     */
    public T getService(String className) {
        return services.get(className);
    }

    /**
     * {@inheritDoc}
     */
    public boolean addService(T service) {
        Assert.notNull(service, "service");
        final T existingService = services.put(service.getClass().getName(), service);
        if (existingService == service) {
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
    public List<ServiceRegistryListener<T>> getListeners() {
        return (List<ServiceRegistryListener<T>>) listeners.clone();
    }

    /**
     * {@inheritDoc}
     */
    public void addListener(ServiceRegistryListener<T> listener) {
        Assert.notNull(listener, "listener");
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeListener(ServiceRegistryListener<T> listener) {
        Assert.notNull(listener, "listener");
        listeners.remove(listener);
    }
}
