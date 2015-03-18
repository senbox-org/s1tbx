package com.bc.ceres.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The default resource locator. Locates resources using the current context class loader.
 * Can be easily subclassed by overriding {@link #getResourceClassLoader()} in order to return an appropriate class loader.
 */
public class DefaultResourceLocator extends ResourceLocator {

    public static final Logger LOG = Logger.getLogger(DefaultResourceLocator.class.getName());

    @Override
    public Collection<Path> locateResources(String name) {
        Set<Path> resourcesSet = new HashSet<>();
        try {
            Enumeration<URL> resources = getResourceClassLoader().getResources(name);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                URI uri = toUri(resource);
                if (uri != null) {
                    resourcesSet.add(toPath(uri));
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, String.format("Failed to retrieve resources for name '%s'", name), e);
        }
        return resourcesSet;
    }

    /**
     * The default implementation returns the context class loader.
     * @return the class loader used to load resources.
     */
    protected ClassLoader getResourceClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    protected Path toPath(URI uri) throws IOException {
        try {
            return Paths.get(uri);
        } catch (FileSystemNotFoundException exp) {
            FileSystems.newFileSystem(uri, Collections.emptyMap());
            return Paths.get(uri);
        }
    }

    private URI toUri(URL resource) {
        try {
            return resource.toURI();
        } catch (URISyntaxException e) {
            LOG.log(Level.WARNING, String.format("Ignoring malformed resource URI '%s'", resource), e);
            return null;
        }
    }
}
