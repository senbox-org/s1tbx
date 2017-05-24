package org.esa.snap.runtime;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This {@link #getInstance() singleton} class is used to run client code that uses the various SNAP Engine APIs.
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

    private enum Lifecycle {
        START, STOP
    }

    private static Engine instance;
    private final ClassLoader clientClassLoader;

    private Engine(boolean standAloneMode) {
        getConfig().load();
        if (standAloneMode) {
            long t0 = System.currentTimeMillis();
            InstallationScanner.ScanResult scanResult = new InstallationScanner(getConfig()).scanInstallationDir();
            long t1 = System.currentTimeMillis();
            if (getConfig().debug()) {
                getLogger().info("Scanning of installation directory took " + (t1 - t0) + " ms");
            }
            setJavaLibraryPath(scanResult.libraryPathEntries);
            clientClassLoader = createClientClassLoader(scanResult.classPathEntries);
        } else {
            clientClassLoader = Thread.currentThread().getContextClassLoader();
        }
    }

    /**
     * @return The runtime singleton instance, if the SNAP Engine has been started, {@code null} otherwise.
     *
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
     * It is recommended to call {@link #stop()} once the Engine is no longer required by any client code.
     *
     * @return The runtime singleton instance.
     *
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
     *
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
                    instance.runClientCode(() -> instance.informActivators(Lifecycle.START));
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
        if (instance != null) {
            instance.runClientCode(() -> instance.informActivators(Lifecycle.STOP));
            instance = null;
        }
    }

    /**
     * Returns a class loader providing access to SNAP Engine classes and resources.
     * A typical and effective use is to make it current thread's context class loader:
     * <pre>
     *  Thread.currentThread().setContextClassLoader(Engine.getInstance().getClientClassLoader());
     *  </pre>
     *
     * @return The class loader providing access to SNAP Engine classes and resources.
     *
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
     *
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
     *
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
     *
     * @see #getClientClassLoader()
     */
    public Runnable createClientRunnable(Runnable runnable) {
        assertStarted();
        return () -> runClientCode(runnable);
    }

    private void informActivators(Lifecycle lifecycle) {
        ServiceLoader<Activator> activators = ServiceLoader.load(Activator.class, clientClassLoader);
        List<Activator> activatorList = new ArrayList<>();
        for (Activator activator : activators) {
            activatorList.add(activator);
        }
        activatorList.sort(
                (a1, a2) -> lifecycle == Lifecycle.START ? a1.getStartLevel() - a2.getStartLevel() : a2.getStartLevel() - a1.getStartLevel());
        for (Activator activator : activatorList) {
            try {
                if (lifecycle == Lifecycle.START) {
                    activator.start();
                } else {
                    activator.stop();
                }
            } catch (Exception ex) {
                getConfig().logger().log(Level.SEVERE, String.format("Failed to %s %s",
                                                                     lifecycle == Lifecycle.START ? "start" : "stop",
                                                                     activator.getClass().getName()), ex);
            }
        }
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

    private void traceClassLoader(String name, ClassLoader classLoader) {
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

    private void traceLibraryPaths() {
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

    private void assertStarted() {
        if (instance == null) {
            throw new IllegalStateException("Please call " + Engine.class + ".start() first.");
        }
    }


}
