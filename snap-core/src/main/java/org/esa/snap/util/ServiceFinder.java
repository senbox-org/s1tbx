package org.esa.snap.util;

import com.bc.ceres.core.ResourceLocator;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.esa.snap.util.SystemUtils.LOG;

/**
 * A finder for service provider interface (SPI) registries.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public class ServiceFinder {

    private final String servicesPath;
    private final List<Path> searchPaths;
    private boolean searchClassPath;

    public ServiceFinder(String serviceName) {
        servicesPath = "META-INF/services/" + serviceName;
        searchPaths = new ArrayList<>();
    }

    /**
     * The module containing the services.
     */
    public static class Module {
        private final Path path;
        private final List<String> serviceNames;

        private Module(Path path, List<String> serviceNames) {
            this.path = path;
            this.serviceNames = serviceNames;
        }

        /**
         * The module's path.
         */
        public Path getPath() {
            return path;
        }

        /**
         * The service names parsed from the module's service registry file.
         */
        public List<String> getServiceNames() {
            return Collections.unmodifiableList(serviceNames);
        }
    }

    /**
     * Adds a search (directory) path.
     *
     * @param path A search path.
     */
    public void addSearchPath(Path path) {
        searchPaths.add(path);
    }

    /**
     * Adds search (directory) paths.
     *
     * @param paths The search paths.
     */
    public void addSearchPaths(Path... paths) {
        searchPaths.addAll(Arrays.asList(paths));
    }

    /**
     * Adds search paths from user preferences.
     * The value is be a colon- (Unix) or semicolon-separated list of (directory) paths.
     *
     * @param configName The configuration name.
     * @param key        The user preferences key.
     */
    public void addSearchPathsFromPreferences(String configName, String key) {
        addSearchPathsFromPreferencesValue(Config.instance(configName).preferences().get(key, null));
    }

    /**
     * Adds search paths from user preferences.
     * The value is be a colon- (Unix) or semicolon-separated list of (directory) paths.
     *
     * @param key The user preferences key.
     */
    public void addSearchPathsFromPreferences(String key) {
        addSearchPathsFromPreferencesValue(Config.instance().preferences().get(key, null));
    }

    /**
     * @param searchClassPath {@code true}, if the Java classpath shall be searched as well
     */
    public void searchClassPath(boolean searchClassPath) {
        this.searchClassPath = searchClassPath;
    }

    /**
     * Finds services based on the current search path configuration.
     *
     * @return List of modules providing the services.
     */
    public List<Module> findServices() {
        List<Module> modules = new ArrayList<>();
        for (Path directory : searchPaths) {
            scanPath(directory, modules);
        }
        if (searchClassPath) {
            scanClassPath(modules);
        }
        return modules;
    }

    private void addSearchPathsFromPreferencesValue(String extraPaths) {
        if (extraPaths != null) {
            addSearchPaths(Stream.of(extraPaths.split(File.pathSeparator))
                                   .map(s -> Paths.get(s))
                                   .toArray(Path[]::new));
        }
    }

    private void scanPath(Path path, List<Module> modules) {
        if (Files.isDirectory(path)) {
            scanDirectory(path, modules);
        } else {
            LOG.warning("Can't search for SPIs, not a directory: " + path);
        }
    }

    private void scanDirectory(Path directory, List<Module> modules) {
        try {
            LOG.fine("Searching for SPIs " + servicesPath + " in " + directory);
            Files.list(directory).forEach(entry -> {
                // Note we may allow for zip/jar files here later!
                parseServiceRegistry(entry.resolve(servicesPath), modules);
            });
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to list directory: " + directory, e);
        }
    }

    private void scanClassPath(List<Module> modules) {
        LOG.fine("Searching for SPIs " + servicesPath + " in Java class path");
        Collection<Path> resources = ResourceLocator.getResources(servicesPath);
        resources.forEach(path -> parseServiceRegistry(path, modules));
    }

    private void parseServiceRegistry(Path registryPath, List<Module> modules) {
        if (!Files.exists(registryPath) || !registryPath.endsWith(this.servicesPath)) {
            return;
        }

        Path moduleRoot = subtract(registryPath, Paths.get(this.servicesPath).getNameCount());

        ArrayList<String> services = new ArrayList<>();
        try {
            Files.lines(registryPath).forEach(line -> {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    services.add(line);
                }
            });
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to parse service registry file " + registryPath, e);
        }

        if (!services.isEmpty()) {
            modules.add(new Module(moduleRoot, services));
        }
    }

    private static Path subtract(Path resourcePath, int nameCount) {
        Path moduleRoot = resourcePath;
        for (int i = 0; i < nameCount; i++) {
            moduleRoot = moduleRoot.resolve("..");
        }
        moduleRoot = moduleRoot.normalize();
        return moduleRoot;
    }
}
