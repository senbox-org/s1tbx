package com.bc.ceres.core;

import java.nio.file.Path;
import java.util.Collection;
import java.util.ServiceLoader;

/**
 * Resource locators are service providers used collect resources across multiple code bases in multi-module environments.
 *
 * @author Norman Fomferra
 * @since Ceres 2.0
 */
public abstract class ResourceLocator {

    /**
     * Gets all resources from all registered {@link ResourceLocator} services for the given name.
     * @param name The resource name
     * @return The collection of all resources found
     */
    public static Collection<Path> getResources(String name) {
        ServiceLoader<ResourceLocator> providers = ServiceLoader.load(ResourceLocator.class);
        ResourceLocator resourceLocator = null;
        for (ResourceLocator provider : providers) {
            if (!provider.getClass().equals(DefaultResourceLocator.class)) {
                resourceLocator = provider;
                break;
            } else if (resourceLocator == null) {
                resourceLocator = provider;
            }
        }
        if (resourceLocator == null) {
            resourceLocator = new DefaultResourceLocator();
        }
        return resourceLocator.locateResources(name);
    }

    /**
     * Locates all resources with the given name.
     * @param name The resource name
     * @return The collection of all resources located
     */
    public abstract Collection<Path> locateResources(String name);
}
