package org.esa.snap.runtime;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This {@link #getInstance() singleton} class is used run client code that uses the various SNAP Engine APIs.
 * <p>
 * Instances of this class are created using the {@link #start} methods.
 * <p>
 * Note that using this class is not required if you develop SNAP plugins.
 *
 * @author Norman Fomferra
 * @see Config
 * @see Launcher
 * @since SNAP 2.0
 */
public class Engine {

    private static final String JAR_EXT = ".jar";

    private static Engine instance;
    private final ClassLoader clientClassLoader;

    private Engine(boolean standAloneMode) {
        getConfig().load();
        if (standAloneMode) {
            ScanResult scanResult = scanInstallationDir();
            setJavaLibraryPath(scanResult.libraryPathEntries);
            clientClassLoader = createClientClassLoader(scanResult.classPathEntries);
        } else {
            clientClassLoader = Thread.currentThread().getContextClassLoader();
        }
    }

    /**
     * @return The runtime singleton instance, if the SNAP Engine has been started, {@code null} otherwise.
     * @see #start()
     * @see #stop()
     */
    public static Engine getInstance() {
        return instance;
    }

    /**
     * @return The SNAP-Engine configuration.
     */
    public EngineConfig getConfig() {
        return EngineConfig.instance();
    }

    /**
     * @return The SNAP-Engine logger.
     */
    public Logger getLogger() {
        return getConfig().logger();
    }

    /**
     * Starts the SNAP Engine. If the Engine has already been started, no further actions are performed and
     * the existing instance is returned.
     * <p>
     * The method simply calls {@link #start(boolean) start(true)}.
     * <p>
     * It is recommended to call {@link #stop()} once the Engine is longer required by any client code.
     *
     * @return The runtime singleton instance.
     * @see #start(boolean)
     */
    public static Engine start() {
        return start(true);
    }

    /**
     * Starts the SNAP Engine Runtime. If the Engine has already been started, no further actions are performed and
     * the existing instance is returned.
     * <p>
     * If the given {@code setContextClassLoader} parameter is {@code true}, the current thread's context class loader
     * will become the Engine's {@link #getClientClassLoader() client class loader}.
     * This may be the desired behaviour for many use cases.
     * <p>
     * If the given {@code setContextClassLoader} parameter is {@code false}, the current thread's class loader
     * remains unchanged. However, you can use various utility methods to apply it: {@link #setContextClassLoader()},
     * {@link #runClientCode(Runnable)}, {@link #createClientRunnable(Runnable)}.
     * <p>
     * It is recommended to call {@link #stop()} once the Engine is longer required by any client code.
     *
     * @param setContextClassLoader If {@code true}, the {@link #getClientClassLoader() client
     *                              class loader} will become the current thread's context class loader.
     * @return The runtime singleton instance
     * @see #start()
     */
    public static Engine start(boolean setContextClassLoader) {
        if (instance == null) {
            synchronized (Engine.class) {
                if (instance == null) {
                    instance = new Engine(setContextClassLoader);
                    if (setContextClassLoader) {
                        instance.setContextClassLoader();
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Stops the SNAP Engine Runtime. Calling this method on a stopped Engine has no effect.
     *
     * @see #start()
     * @see #start(boolean)
     */
    public synchronized void stop() {
        instance = null;
    }

    /**
     * Returns a class loader providing access to SNAP Engine classes and resources.
     * A typical and effective use is to make it current thread's context class loader:
     * <pre>
     *  Thread.currentThread().setContextClassLoader(Engine.getInstance().getClientClassLoader());
     *  </pre>
     *
     * @return The class loader providing access to SNAP Engine classes and resources.
     * @throws IllegalStateException If {@link #stop()} has already been called.
     * @see #setContextClassLoader()
     */
    public ClassLoader getClientClassLoader() {
        assertStarted();
        return clientClassLoader;
    }

    /**
     * Utility method which is a shortcut for:
     * <pre>
     *  Thread.currentThread().setContextClassLoader(Engine.getInstance().getClientClassLoader());
     *  </pre>
     *
     * @return The previous context class loader.
     * @throws IllegalStateException If {@link #stop()} has already been called.
     * @see #getClientClassLoader()
     */
    public ClassLoader setContextClassLoader() {
        assertStarted();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clientClassLoader);
        return contextClassLoader;
    }

    /**
     * Runs the given Runnable with the {@link #getClientClassLoader() client class loader} as the
     * current thread's context class loader.
     *
     * @param runnable Client code to be executed using the client class loader.
     * @return This instance so that calls can be chained in a single expression.
     * @throws IllegalStateException If {@link #stop()} has already been called.
     */
    public Engine runClientCode(Runnable runnable) {
        assertStarted();
        ClassLoader classLoader = setContextClassLoader();
        try {
            runnable.run();
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        return this;
    }

    /**
     * Creates a new runnable which will run the given runnable using the {@link #getClientClassLoader() client class loader}.
     * <p>
     * Invoking the returned Runnable may cause an {@code IllegalStateException} if {@link #stop()} has already been called.
     *
     * @param runnable Client code to be executed using the client class loader.
     * @return A new runnable which will delegate to the given runnable.
     * @see #getClientClassLoader()
     */
    public Runnable createClientRunnable(Runnable runnable) {
        assertStarted();
        return () -> runClientCode(runnable);
    }

    private ClassLoader createClientClassLoader(List<Path> paths) {

        List<URL> urls = new ArrayList<>();
        for (Path path : paths) {
            try {
                URL url = path.toUri().toURL();
                urls.add(url);
            } catch (MalformedURLException e) {
                getLogger().severe(e.getMessage());
            }
        }

        ClassLoader classLoader = getClass().getClassLoader();
        if (!urls.isEmpty()) {
            classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), classLoader);
        }

        if (getConfig().debug()) {
            traceClassLoader("classLoader", classLoader);
        }

        return classLoader;
    }

    private void setJavaLibraryPath(List<Path> paths) {

        String javaLibraryPath = System.getProperty("java.library.path");
        String extraLibraryPath = paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
        if (javaLibraryPath == null || javaLibraryPath.isEmpty()) {
            setJavaLibraryPath(extraLibraryPath);
        } else if (!extraLibraryPath.isEmpty()) {
            setJavaLibraryPath(extraLibraryPath + File.pathSeparator + javaLibraryPath);
        }

        if (getConfig().debug()) {
            traceLibraryPaths();
        }
    }

    private void setJavaLibraryPath(String extraLibraryPath) {

        if (!getConfig().setSystemProperty("java.library.path", extraLibraryPath)) {
            return;
        }

        try {
            //
            // The following hack is based on an implementation detail of the Oracle ClassLoader implementation.
            // It checks whether its static field "sys_path" is null, and if so it sets its static field "user_paths"
            // to the parsed value of system property "java.library.path" and caches it. This behaviour prevents it
            // from accepting any programmatical changes of system property "java.library.path".
            //
            Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            getLogger().log(Level.SEVERE, "Failed to modify class loader field 'sys_paths'", e);
        }
    }

    private ScanResult scanInstallationDir() {
        try {
            long t0 = System.currentTimeMillis();
            ScanResult scanResult = scanInstallationDir0();
            long t1 = System.currentTimeMillis();
            if (getConfig().debug()) {
                getLogger().info("Scanning of installation directory took " + (t1 - t0) + " ms");
            }
            return scanResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ScanResult scanInstallationDir0() throws IOException {
        ScanResult scanResult = new ScanResult();
        Path installationDir = getConfig().installDir();
        Path clustersFile = installationDir.resolve(Paths.get("etc", "snap.clusters"));
        if (Files.exists(clustersFile)) {
            // SNAP-Desktop NetBeans installation (the default)
            return scanNetBeansInstallationStructure(installationDir, clustersFile, scanResult);
        } else {
            // SNAP-Engine stand-alone packaging
            return scanEngineInstallationStructure(installationDir, scanResult);
        }
    }

    private ScanResult scanEngineInstallationStructure(Path installationDir, ScanResult scanResult) throws IOException {
        Path modulesDir = installationDir.resolve("modules");
        if (Files.isDirectory(modulesDir)) {
            scanDir(modulesDir, scanResult);
        }
        Path libDir = installationDir.resolve("lib");
        if (Files.isDirectory(libDir)) {
            scanDir(libDir, scanResult);
        }
        return scanResult;
    }

    private ScanResult scanNetBeansInstallationStructure(Path installationDir, Path clustersFile, ScanResult scanResult) throws IOException {

        Set<String> excludedClusterNames = new HashSet<>();
        Collections.addAll(excludedClusterNames, getConfig().excludedClusterNames());

        ArrayList<Path> clusterPaths = new ArrayList<>();
        try {
            List<String> clusterNames = Files.readAllLines(clustersFile);
            clusterNames.stream().filter(clusterName -> !excludedClusterNames.contains(clusterName)).forEach(clusterName -> {
                Path clusterPath = installationDir.resolve(clusterName);
                if (Files.isDirectory(clusterPath)) {
                    clusterPaths.add(clusterPath);
                }
            });
        } catch (IOException e) {
            fail(e);
        }


        Set<String> excludedModuleNames = new HashSet<>();
        String[] moduleNames = getConfig().excludedModuleNames();
        for (String mavenName : moduleNames) {
            if (mavenName.indexOf(':') == -1) {
                mavenName = "org.esa.snap:" + mavenName;
            }
            String netBeansName = mavenName.replace(':', '-').replace('.', '-');
            excludedModuleNames.add(netBeansName + ".jar");
        }

        if (!clusterPaths.isEmpty()) {
            for (Path clusterPath : clusterPaths) {
                scanNetBeansCluster(clusterPath, excludedModuleNames, scanResult);
            }
        } else {
            fail("No classpath entries found");
        }

        return scanResult;
    }

    private void scanNetBeansCluster(Path clusterDir, Set<String> excludedModuleNames, ScanResult scanResult) throws IOException {
        Path modulesDir = clusterDir.resolve(Paths.get("modules"));

        // Collect module JARs
        List<Path> moduleJarFiles = Files.list(modulesDir)
                .filter(path -> Files.isRegularFile(path))
                .filter(path -> path.endsWith(JAR_EXT))
                .filter((path) -> !excludedModuleNames.contains(path.getFileName().toString()))
                .collect(Collectors.toList());
        for (Path moduleJarFile : moduleJarFiles) {
            scanResult.classPathEntries.add(moduleJarFile);
        }

        // Retrieve list of included module names
        Set<String> includedModuleNames = new HashSet<>();
        for (Path moduleJarFile : moduleJarFiles) {
            String moduleJarName = moduleJarFile.getFileName().toString();
            String moduleName = moduleJarName.substring(0, moduleJarName.length() - JAR_EXT.length()).replace('.', '-');
            includedModuleNames.add(moduleName);
        }

        // Collect external JAR dependencies for each included module
        Path extDir = modulesDir.resolve(Paths.get("ext"));
        if (Files.isDirectory(extDir)) {
            List<Path> subDirs = Files.list(extDir).filter(p -> Files.isDirectory(p)).collect(Collectors.toList());
            for (Path subDir : subDirs) {
                String moduleName = subDir.getFileName().toString().replace('.', '-');
                if (includedModuleNames.contains(moduleName)) {
                    scanDir(subDir, scanResult);
                }
            }
        }

        // Collect native library dependencies
        scanNativeLibraryPaths(modulesDir, scanResult);
    }

    private void scanNativeLibraryPaths(Path modulesDir, ScanResult scanResult) {
        Path libDir = modulesDir.resolve(Paths.get("lib"));
        if (Files.isDirectory(libDir)) {
            scanResult.libraryPathEntries.add(libDir);
            Path libArchDir = libDir.resolve(System.getProperty("os.arch"));
            if (Files.isDirectory(libArchDir)) {
                scanResult.libraryPathEntries.add(libArchDir);
                Path libArchOsDir = libArchDir.resolve(System.getProperty("os.name"));
                if (Files.isDirectory(libArchOsDir)) {
                    scanResult.libraryPathEntries.add(libArchOsDir);
                }
            }
        }
    }

    private void scanDir(Path dir, ScanResult scanResult) throws IOException {
        List<Path> entries = Files.list(dir).collect(Collectors.toList());

        scanResult.classPathEntries.addAll(entries.stream()
                                                   .filter(path -> Files.isRegularFile(path))
                                                   .filter(path -> path.getFileName().toString().endsWith(JAR_EXT))
                                                   .collect(Collectors.toList()));

        for (Path entry : entries) {
            if (Files.isDirectory(entry)) {
                scanDir(entry, scanResult);
            }
        }
    }

    private void fail(String s) {
        throw new RuntimeException(s);
    }

    private void fail(Exception e) {
        throw new RuntimeException(e);
    }

    private void traceClassLoader(String name, ClassLoader classLoader) {
        if (getConfig().debug()) {
            Logger logger = getLogger();
            logger.info(name + ".class = " + classLoader.getClass() + " =========================================================");
            if (classLoader instanceof URLClassLoader) {
                URL[] classpath = ((URLClassLoader) classLoader).getURLs();
                for (int i = 0; i < classpath.length; i++) {
                    logger.info(name + ".url[" + i + "] = " + classpath[i]);
                }
            }
            if (classLoader.getParent() != null) {
                traceClassLoader(name + ".parent", classLoader.getParent());
            } else {
                logger.info(name + ".parent = null");
            }
        }
    }

    private void traceLibraryPaths() {
        if (getConfig().debug()) {
            Logger logger = getLogger();
            String[] paths = System.getProperty("java.library.path", "").split(File.pathSeparator);
            if (paths.length > 0) {
                logger.info("JNI library paths:");
                for (int i = 0; i < paths.length; i++) {
                    String path = paths[i];
                    logger.info("java.library.path[" + i + "] = " + path);
                }
            } else {
                logger.info("JNI library paths: none");
            }
        }
    }

    private void assertStarted() {
        if (instance == null) {
            throw new IllegalStateException("Please call " + Engine.class + ".start() first.");
        }
    }

    private static class ScanResult {
        List<Path> classPathEntries;
        List<Path> libraryPathEntries;

        ScanResult() {
            classPathEntries = new ArrayList<>();
            libraryPathEntries = new ArrayList<>();
        }
    }

}
