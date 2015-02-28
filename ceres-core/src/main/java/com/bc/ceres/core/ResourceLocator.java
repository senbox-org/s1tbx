package com.bc.ceres.core;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;

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
        Set<Path> resources = new HashSet<>();
        ServiceLoader<ResourceLocator> providers = ServiceLoader.load(ResourceLocator.class);
        providers.forEach(new Consumer<ResourceLocator>() {
            @Override
            public void accept(ResourceLocator resourceLocator) {
                resources.addAll(resourceLocator.locateResources(name));
            }
        });
        return resources;
    }

    /**
     * Locates all resources with the given name.
     * @param name The resource name
     * @return The collection of all resources located
     */
    public abstract Collection<Path> locateResources(String name);
}
