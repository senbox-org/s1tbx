package com.bc.ceres.core;

import com.bc.ceres.core.ServiceRegistry;

import java.util.*;

/**
 * A manager for service registries.
 * @since 0.6
 * @deprecated since Ceres 0.10, use {@link com.bc.ceres.core.ServiceRegistryManager} instead
 */
@Deprecated
public class ServiceRegistryFactory {

    private HashMap<Class, ServiceRegistry> serviceRegistries = new HashMap<Class, ServiceRegistry>(10);
    private static ServiceRegistryFactory instance = new ServiceRegistryFactory();

    public static ServiceRegistryFactory getInstance() {
        return instance;
    }

    public static void setInstance(ServiceRegistryFactory instance) {
        Assert.notNull(instance, "instance");
        ServiceRegistryFactory.instance = instance;
    }

    public <T> ServiceRegistry<T> getServiceRegistry(Class<T> serviceType) {
        Assert.notNull(serviceType, "serviceType");
        ServiceRegistry<T> serviceRegistry = serviceRegistries.get(serviceType);
        if (serviceRegistry == null) {
            serviceRegistry = createServiceRegistry(serviceType);
            serviceRegistries.put(serviceType, serviceRegistry);
        }
        return serviceRegistry;
    }

    protected <T> ServiceRegistry<T> createServiceRegistry(Class<T> serviceType) {
        Assert.notNull(serviceType, "serviceType");
        return new DefaultServiceRegistry<T>(serviceType);
    }
}