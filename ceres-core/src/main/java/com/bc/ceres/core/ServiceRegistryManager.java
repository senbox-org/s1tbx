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
        serviceRegistries = new HashMap<Class, ServiceRegistry>(10);
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
        return new DefaultServiceRegistry<T>(serviceType);
    }
}
