package com.bc.ceres.core;

import java.util.*;

/**
 * @since 0.6
 */
public class DefaultServiceRegistry<T> implements ServiceRegistry<T> {
    private final Class<T> serviceType;
    private HashSet<T> services = new HashSet<T>(10);
    private ArrayList<ServiceRegistryListener<T>> listeners = new ArrayList<ServiceRegistryListener<T>>(3);
    private boolean metaInfServicesInit;

    public DefaultServiceRegistry(Class<T> serviceType) {
        Assert.notNull(serviceType, "serviceType");
        this.serviceType = serviceType;
    }

    public Class<T> getServiceType() {
        return serviceType;
    }

    public Set<T> getServices() {
        maybeAddMetaInfServices();
        return (Set<T>) services.clone();
    }

    public T getService(String className) {
        maybeAddMetaInfServices();
        for (T service : services) {
            if (service.getClass().getName().equals(className)) {
                return service;
            }
        }
        return null;
    }

    public void addService(T service) {
        Assert.notNull(service, "service");
        maybeAddMetaInfServices();
        if (services.add(service)) {
            for (ServiceRegistryListener<T> listener : listeners) {
                listener.serviceAdded(this, service);
            }
        }
    }

    public void removeService(T service) {
        Assert.notNull(service, "service");
        maybeAddMetaInfServices();
        if (services.remove(service)) {
            for (ServiceRegistryListener<T> listener : listeners) {
                listener.serviceRemoved(this, service);
            }
        }
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

    private void maybeAddMetaInfServices() {
        if (!metaInfServicesInit) {
            metaInfServicesInit = true;
            addMetaInfServices();
        }
    }

    private void addMetaInfServices() {
        ServiceLoader sl = ServiceLoader.load(getServiceType());
        sl.reload();
        for (Object service : sl) {
            addService((T) service);
        }
    }
}
