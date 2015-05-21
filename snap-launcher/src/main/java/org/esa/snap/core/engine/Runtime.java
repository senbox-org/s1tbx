package org.esa.snap.core.engine;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This {@link #instance() singleton} class is used run client code that uses the SNAP Engine Java API.
 * <p>
 * The easiest way to use SNAP Engine code is to surround it by the two methods {@link #pushContextClassLoader()}
 * and {@link #popContextClassLoader()}. Smaller code snippets can be run using the {@link #runInContext(Runnable)} methods.
 * Finally it is possible to use the SNAP Engine's {@link #getClassLoader()} class loader} directly.
 * <p>
 * Note that using this class is not required if you develop SNAP Engine plugins.
 *
 * @author Norman Fomferra
 * @see Config
 * @see Launcher
 * @since SNAP 2.0
 */
public class Runtime {

    private static final String JAR_EXT = ".jar";

    private static final Runtime instance = new Runtime();
    private ClassLoader classLoader;
    private Deque<ClassLoader> contextClassLoaderStack;
    private Set<String> excludedClusterNames;
    private Set<String> excludedModuleNames;

    private Runtime() {
    }

    /**
     * @return The singleton instance of this class.
     */
    public static Runtime instance() {
        return instance;
    }

    /**
     * @return The configuration.
     */
    public Config getConfig() {
        return Config.instance();
    }

    /**
     * @return A configured logger.
     */
    public Logger getLogger() {
        return Config.instance().logger();
    }

    /**
     * @return A configured class loader providing access to SNAP Engine classes and resources.
     */
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            synchronized (this) {
                if (classLoader == null) {
                    getConfig().load();
                    ScanResult scanResult = scanInstallationDir();
                    setJavaLibraryPath(scanResult.libraryPathEntries);
                    classLoader = createClassLoader(scanResult.classPathEntries);
                }
            }
        }
        return classLoader;
    }

    /**
     * Pushes the current context class loader onto a stack and makes this Runtime's class loader the
     * current context class loader.
     *
     * @see #popContextClassLoader()
     * @see #runInContext(Runnable)
     * @see #getClassLoader()
     */
    public synchronized void pushContextClassLoader() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader newClassLoader = getClassLoader();
        if (contextClassLoaderStack == null) {
            contextClassLoaderStack = new ArrayDeque<>();
        }
        contextClassLoaderStack.add(oldClassLoader);
        Thread.currentThread().setContextClassLoader(newClassLoader);
    }

    /**
     * Pops a class loader from a stack and makes it the
     * current context class loader.
     *
     * @see #pushContextClassLoader()
     * @see #runInContext(Runnable)
     * @see #getClassLoader()
     */
    public synchronized void popContextClassLoader() {
        if (contextClassLoaderStack == null || contextClassLoaderStack.isEmpty()) {
            throw new IllegalStateException("contextClassLoaderStack");
        }
        ClassLoader oldClassLoader = contextClassLoaderStack.pop();
        Thread.currentThread().setContextClassLoader(oldClassLoader);
    }

    /**
     * Runs the given Runnable with this Runtime's class loader as the
     * current context class loader.
     *
     * @see #pushContextClassLoader()
     * @see #popContextClassLoader()
     * @see #getClassLoader()
     */
    public void runInContext(Runnable runnable) {
        try {
            pushContextClassLoader();
            runnable.run();
        } finally {
            popContextClassLoader();
        }
    }

    private ClassLoader createClassLoader(List<Path> paths) {

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

        if (Config.instance().debug()) {
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

        if (Config.instance().debug()) {
            traceLibraryPaths();
        }
    }

    private void setJavaLibraryPath(String extraLibraryPath) {

        if (!Config.instance().setSystemProperty("java.library.path", extraLibraryPath)) {
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
            if (Config.instance().debug()) {
                getLogger().info("Scanning of installation directory took " + (t1 - t0) + " ms");
            }
            return scanResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ScanResult scanInstallationDir0() throws IOException {
        ScanResult scanResult = new ScanResult();
        Path installationDir = Config.instance().homeDir();
        Path clustersFile = installationDir.resolve(Paths.get("etc", "snap.clusters"));
        if (Files.exists(clustersFile)) {
            // SNAP-Desktop NetBeans installation (the default)
            return scanNetBeansInstallationStructure(scanResult, installationDir, clustersFile);
        } else {
            // SNAP-Engine stand-alone packaging
            return scanEngineInstallationStructure(scanResult, installationDir);
        }
    }

    private ScanResult scanEngineInstallationStructure(ScanResult scanResult, Path installationDir) throws IOException {
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

    private ScanResult scanNetBeansInstallationStructure(ScanResult scanResult, Path installationDir, Path clustersFile) throws IOException {

        ArrayList<Path> clusterPaths = new ArrayList<>();
        try {
            List<String> clusters = Files.readAllLines(clustersFile);
            for (String cluster : clusters) {
                if (!isExcludedClusterName(cluster)) {
                    Path clusterPath = installationDir.resolve(cluster);
                    if (Files.isDirectory(clusterPath)) {
                        clusterPaths.add(clusterPath);
                    }
                }
            }
        } catch (IOException e) {
            fail(e);
        }

        if (!clusterPaths.isEmpty()) {
            for (Path clusterPath : clusterPaths) {
                scanNetBeansCluster(clusterPath, scanResult);
            }
        } else {
            fail("No classpath entries found");
        }

        return scanResult;
    }

    private boolean isExcludedClusterName(String clusterName) {
        if (excludedClusterNames == null) {
            excludedClusterNames = new HashSet<>();
            Collections.addAll(excludedClusterNames, Config.instance().excludedClusterNames());
        }
        return excludedClusterNames.contains(clusterName);
    }


    private boolean isExcludedModuleJarName(String jarName) {
        if (excludedModuleNames == null) {
            excludedModuleNames = new HashSet<>();
            String[] moduleNames = Config.instance().excludedModuleNames();
            for (String mavenName : moduleNames) {
                if (mavenName.indexOf(':') == -1) {
                    mavenName = "org.esa.snap:" + mavenName;
                }
                String netBeansName = mavenName.replace(':', '-').replace('.', '-');
                excludedModuleNames.add(netBeansName + ".jar");
            }
        }
        return excludedModuleNames.contains(jarName);
    }

    private void scanNetBeansCluster(Path clusterDir, ScanResult scanResult) throws IOException {
        Path modulesDir = clusterDir.resolve(Paths.get("modules"));

        // Collect module JARs
        List<Path> moduleJarFiles = Files.list(modulesDir).filter(this::isIncludedModuleJar).collect(Collectors.toList());
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

        scanResult.classPathEntries.addAll(entries.stream().filter(this::isJar).collect(Collectors.toList()));

        for (Path entry : entries) {
            if (Files.isDirectory(entry)) {
                scanDir(entry, scanResult);
            }
        }
    }

    private boolean isJar(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(JAR_EXT);
    }

    private boolean isIncludedModuleJar(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String jarName = path.getFileName().toString();
        return jarName.endsWith(JAR_EXT) && !isExcludedModuleJarName(jarName);
    }

    private void fail(String s) {
        throw new RuntimeException(s);
    }

    private void fail(Exception e) {
        throw new RuntimeException(e);
    }

    private void traceClassLoader(String name, ClassLoader classLoader) {
        if (Config.instance().debug()) {
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
        if (Config.instance().debug()) {
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

    private static class ScanResult {
        List<Path> classPathEntries;
        List<Path> libraryPathEntries;

        ScanResult() {
            classPathEntries = new ArrayList<>();
            libraryPathEntries = new ArrayList<>();
        }
    }

}
