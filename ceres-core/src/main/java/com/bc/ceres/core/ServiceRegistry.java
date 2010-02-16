package com.bc.ceres.core;

import java.util.List;
import java.util.Set;

/**
 * A registry for services of a specific type.
 *
 * @param <T> The service type. All services are instances of that type.
 * @since 0.6
 */
public interface ServiceRegistry<T> {

    /**
     * Gets the service type. All services in this registry are instances of this type.
     *
     * @return The service type.
     */
    Class<T> getServiceType();

    /**
     * Gets all registered services.
     *
     * @return A set of all services.
     */
    Set<T> getServices();

    /**
     * Gets a registered service instance for the given class name.
     *
     * @param className The name of the service's class.
     * @return The service instance or {@code null} if no such exists.
     */
    T getService(String className);

    /**
     * Adds a new service to this registry. The method will automatically remove
     * an already registered service of the same type. If the regsitry changes
     * due to a call of this method, a change event will be fired.
     *
     * @param service The service to be added.
     * @return {@code true} if the service has been added.
     */
    boolean addService(T service);

    /**
     * Removes an existing service from this registry. If the regsitry changes
     * due to a call of this method, a change event will be fired.
     *
     * @param service The service to be removed.
     * @return {@code true} if the service has been removed.
     */
    boolean removeService(T service);

    /**
     * @return The list of registry listeners.
     */
    List<ServiceRegistryListener<T>> getListeners();

    /**
     * Adds a new registry listener.
     *
     * @param listener The registry listener to be added.
     */
    void addListener(ServiceRegistryListener<T> listener);

    /**
     * Removes an existing registry listener.
     *
     * @param listener The registry listener to be removed.
     */
    void removeListener(ServiceRegistryListener<T> listener);
}
