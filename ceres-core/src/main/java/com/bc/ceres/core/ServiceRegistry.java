package com.bc.ceres.core;

import java.util.Set;
import java.util.List;

/**
 * @since 0.6
 */
public interface ServiceRegistry<T> {

    Class<T> getServiceType();

    Set<T> getServices();

    T getService(String className);

    boolean addService(T service);

    boolean removeService(T service);

    List<ServiceRegistryListener<T>> getListeners();

    void addListener(ServiceRegistryListener<T> listener);

    void removeListener(ServiceRegistryListener<T> listener);
}
