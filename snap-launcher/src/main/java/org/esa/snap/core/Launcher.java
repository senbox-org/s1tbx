package org.esa.snap.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * This class is used to launch stand-alone SNAP Engine applications.
 */
public class Launcher {

    public static final String JAR_EXT = ".jar";
    private final Path installationDir;
    private final String mainClassName;
    private final boolean debug;
    private final Logger logger;

    // TODO nf 20150520 - read this list from configuration file
    // TODO nf 20150520 - allow for wildcards, e.g. "*-ui.jar"
    private String[] EXCLUDED_JAR_NAMES = new String[]{
            "org-esa-snap-netbeans-docwin.jar",
            "org-esa-snap-netbeans-tile.jar",
            "org-esa-snap-snap-gui-utilities.jar",
            "org-esa-snap-snap-visat-rcp.jar",
            "org-esa-snap-snap-worldwind.jar",
            "org-esa-snap-snap-rcp.jar",
            "org-esa-snap-snap-ui.jar",
            "org-esa-snap-ceres-ui.jar",
            "org-esa-snap-snap-gpf-ui.jar",
            "org-esa-snap-snap-dem-ui.jar",
            "org-esa-snap-snap-pixel-extraction-ui.jar",
            "org-esa-snap-snap-unmix-ui.jar",
            "org-esa-snap-snap-binning-ui.jar",
            "org-esa-snap-snap-collocation-ui.jar",
    };

    public static void main(String[] args) {
        try {
            Launcher launcher = new Launcher();
            launcher.run(args);
        } catch (Throwable e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public Launcher() {
        installationDir = Paths.get(System.getProperty("snap.home", "")).toAbsolutePath();
        mainClassName = System.getProperty("snap.mainClass");
        if (mainClassName == null) {
            throw new IllegalArgumentException("Missing system property 'snap.mainClass'");
        }
        //debug = Boolean.getBoolean("snap.clusters");
        debug = Boolean.getBoolean("snap.debug");
        logger = Logger.getLogger(System.getProperty("snap.logger.name", "org.esa.snap"));
        configureLogger();
    }

    private void configureLogger() {
        String levelName = System.getProperty("snap.logger.level", "INFO");
        try {
            Level level = parseLogLevel(levelName);
            logger.setLevel(level);
        } catch (IllegalArgumentException e) {
            logger.severe("illegal log level '" + levelName + "'");
        } catch (SecurityException e) {
            logger.severe("failed to set log level '" + levelName + "'");
        }
        replaceConsoleLoggerFormatter(Logger.getGlobal());
        replaceConsoleLoggerFormatter(Logger.getLogger(""));
        replaceConsoleLoggerFormatter(this.logger);
    }

    private void replaceConsoleLoggerFormatter(Logger logger) {
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                ConsoleHandler consoleHandler = (ConsoleHandler) handler;
                consoleHandler.setFormatter(new Formatter() {
                    @Override
                    public String format(LogRecord record) {
                        return String.format("%s: %s%n", record.getLevel(), record.getMessage());
                    }
                });
            }
        }
    }

    public void run(String[] args) throws Exception {
        long t0 = System.currentTimeMillis();
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        ScanResult scanResult = scanInstallationDir();
        long t1 = System.currentTimeMillis();
        if (debug) {
            trace("Scanning of installation directory took " + (t1 - t0) + " ms");
        }
        setJavaLibraryPath(scanResult.libraryPathEntries);
        ClassLoader classLoader = createClassLoader(scanResult.classPathEntries);
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            Class<?> mainClass = classLoader.loadClass(mainClassName);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, new Object[]{args});
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }
    }

    public ClassLoader createClassLoader(List<Path> paths) {

        List<URL> urls = new ArrayList<>();
        for (Path path : paths) {
            try {
                URL url = path.toUri().toURL();
                urls.add(url);
            } catch (MalformedURLException e) {
                trace(e.getMessage());
            }
        }

        ClassLoader classLoader = getClass().getClassLoader();
        if (!urls.isEmpty()) {
            classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), classLoader);
        }
        if (debug) {
            traceClassLoader("classLoader", classLoader);
        }
        return classLoader;
    }

    public void setJavaLibraryPath(List<Path> paths) {
        String javaLibraryPath = System.getProperty("java.library.path");
        String extraLibraryPath = paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
        if (javaLibraryPath == null || javaLibraryPath.isEmpty()) {
            System.setProperty("java.library.path", extraLibraryPath);
        } else if (!extraLibraryPath.isEmpty()) {
            System.setProperty("java.library.path", extraLibraryPath + File.pathSeparator + javaLibraryPath);
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
            trace("Warning: Failed to modify class loader: " + e.getMessage());
        }
        if (debug) {
            traceLibraryPaths();
        }
    }

    ScanResult scanInstallationDir() throws IOException {
        ScanResult scanResult = new ScanResult();

        // Check for SNAP-Desktop packaging (the default)

        Path clustersFile = installationDir.resolve(Paths.get("etc", "snap.clusters"));
        if (Files.exists(clustersFile)) {
            ArrayList<Path> clusterPaths = new ArrayList<>();
            try {
                List<String> clusters = Files.readAllLines(clustersFile);
                for (String cluster : clusters) {
                    if (!isExcludedCluster(cluster)) {
                        Path clusterPath = installationDir.resolve(cluster);
                        if (Files.isDirectory(clusterPath)) {
                            clusterPaths.add(clusterPath);
                        }
                    }
                }
            } catch (IOException e) {
                exit(e);
            }
            if (!clusterPaths.isEmpty()) {
                for (Path clusterPath : clusterPaths) {
                    scanCluster(clusterPath, scanResult);
                }
            } else {
                exit("No classpath entries found");
            }

            return scanResult;
        }

        // SNAP-Engine stand-alone packaging

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

    private boolean isExcludedCluster(String cluster) {
        // TODO nf 20150520 - configure excluded clusters
        return cluster.isEmpty() || cluster.equals("ide") || cluster.equals("platform");
    }

    private boolean isExcludedJarName(String jarName) {
        // TODO nf 20150520 - configure excluded JAR names
        for (String excludedJarName : EXCLUDED_JAR_NAMES) {
            if (excludedJarName.equals(jarName)) {
                return true;
            }
        }
        return false;
    }

    private void scanCluster(Path clusterDir, ScanResult scanResult) throws IOException {
        Path modulesDir = clusterDir.resolve(Paths.get("modules"));

        List<Path> modules = Files.list(modulesDir).filter(this::isModuleJar).collect(Collectors.toList());
        for (Path module : modules) {
            scanResult.classPathEntries.add(module);
        }

        Set<String> moduleNames = new HashSet<>();
        for (Path module : modules) {
            String jarName = module.getFileName().toString();
            moduleNames.add(jarName.substring(0, jarName.length() - JAR_EXT.length()).replace('.', '-'));
        }

        Path extDir = modulesDir.resolve(Paths.get("ext"));
        if (Files.isDirectory(extDir)) {
            List<Path> paths = Files.list(extDir).filter(p -> Files.isDirectory(p)).collect(Collectors.toList());
            for (Path path : paths) {
                String moduleName = path.getFileName().toString().replace('.', '-');
                if (moduleNames.contains(moduleName)) {
                    scanDir(path, scanResult);
                }
            }
        }

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

    private boolean isModuleJar(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String jarName = path.getFileName().toString();
        if (jarName.endsWith(JAR_EXT)) {
            if (isExcludedJarName(jarName)) {
                return false;
            }
        }
        return true;
    }

    private void exit(String s) {
        throw new RuntimeException(s);
    }

    private void exit(Exception e) {
        throw new RuntimeException(e);
    }


    private void trace(String msg) {
        if (debug) {
            logger.info(msg);
        }
    }

    // do not delete, useful for debugging

    private void traceClassLoader(String name, ClassLoader classLoader) {
        trace(name + ".class = " + classLoader.getClass() + " =========================================================");
        if (classLoader instanceof URLClassLoader) {
            URL[] classpath = ((URLClassLoader) classLoader).getURLs();
            for (int i = 0; i < classpath.length; i++) {
                trace(name + ".url[" + i + "] = " + classpath[i]);
            }
        }
        if (classLoader.getParent() != null) {
            traceClassLoader(name + ".parent", classLoader.getParent());
        } else {
            trace(name + ".parent = null");
        }
    }

    private void traceLibraryPaths() {
        String[] paths = System.getProperty("java.library.path", "").split(File.pathSeparator);
        if (paths.length > 0) {
            trace("JNI library paths:");
            for (int i = 0; i < paths.length; i++) {
                String path = paths[i];
                trace("java.library.path["+i+"] = " + path);
            }
        } else {
            trace("JNI library paths: none");
        }
    }

    public static Level parseLogLevel(String levelName) throws IllegalArgumentException {
        if ("DEBUG".equalsIgnoreCase(levelName)) {
            return Level.FINE;
        } else if ("ERROR".equalsIgnoreCase(levelName)) {
            return Level.SEVERE;
        } else {
            return Level.parse(levelName);
        }
    }

    static class ScanResult {
        List<Path> classPathEntries;
        List<Path> libraryPathEntries;

        public ScanResult() {
            classPathEntries = new ArrayList<>();
            libraryPathEntries = new ArrayList<>();
        }
    }

}
