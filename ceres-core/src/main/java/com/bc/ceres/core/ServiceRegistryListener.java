package com.bc.ceres.core;

/**
 * @since 0.6
 */
public interface ServiceRegistryListener<T> {

    void serviceAdded(ServiceRegistry<T> registry, T service);

    void serviceRemoved(ServiceRegistry<T> registry, T service);
}
