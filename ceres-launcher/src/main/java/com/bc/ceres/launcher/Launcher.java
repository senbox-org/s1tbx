package com.bc.ceres.launcher;

import com.bc.ceres.launcher.internal.BootstrapClasspathFactory;
import com.bc.ceres.launcher.internal.BruteForceClasspathFactory;
import com.bc.ceres.core.runtime.internal.DefaultRuntimeConfig;
import com.bc.ceres.core.runtime.RuntimeConfig;
import com.bc.ceres.core.runtime.RuntimeConfigException;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * A launcher for applications based on the Ceres runtime.
 * @see RuntimeConfig
 * @see ClasspathFactory
 */
public final class Launcher {
    private RuntimeConfig runtimeConfig;
    private ClasspathFactory classpathFactory;

    /**
     * Lauches the application with a default {@link RuntimeConfig} and a {@link ClasspathFactory}
     * based on the <code>${ceres.context}.mainClass</code> property. If the main class is
     * {@code "com.bc.ceres.core.runtime.RuntimeLauncher"}, then a minimal classpath which at least includes the
     * <code>ceres-core</code> library is used. Otherwise all directories, JARs and ZIPs found in
     * the home directory will be added to the classpath.
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        try {
            Launcher launcher = createDefaultLauncher();
            launcher.launch(args);
        } catch (Throwable e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * Creates a default launcher.
     * @return a default laucher.
     * @throws RuntimeConfigException if the default configuration is invalid
     */
    public static Launcher createDefaultLauncher() throws RuntimeConfigException {
        RuntimeConfig runtimeConfig = new DefaultRuntimeConfig();
        ClasspathFactory classpathFactory;
        if (runtimeConfig.isUsingModuleRuntime()) {
            classpathFactory = new BootstrapClasspathFactory(runtimeConfig);
        } else {
            classpathFactory = new BruteForceClasspathFactory(runtimeConfig);
        }
        return new Launcher(runtimeConfig, classpathFactory);
    }

    /**
     * Constructs a new launcher.
     * @param runtimeConfig the runtime configuration
     * @param classpathFactory the classpath factory
     */
    public Launcher(RuntimeConfig runtimeConfig, ClasspathFactory classpathFactory) {

        this.runtimeConfig = runtimeConfig;
        this.classpathFactory = classpathFactory;

        trace("Configuration type: " + this.runtimeConfig.getClass().getName());
        trace("Classpath type:     " + this.classpathFactory.getClass().getName());
    }

    public RuntimeConfig getRuntimeConfig() {
        return runtimeConfig;
    }

    public ClasspathFactory getClasspathFactory() {
        return classpathFactory;
    }

    public ClassLoader createClassLoader() throws RuntimeConfigException {
        URL[] classpath = createClasspath();
        ClassLoader classLoader = getClass().getClassLoader();
        if (classpath.length > 0) {
            classLoader = new URLClassLoader(classpath, classLoader);
        }
        return classLoader;
    }

    public URL[] createClasspath() throws RuntimeConfigException {
        return classpathFactory.createClasspath();
    }

    public void launch(String[] args) throws Exception {
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = createClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            Class<?> mainClass = classLoader.loadClass(runtimeConfig.getMainClassName());
            if (runtimeConfig.isUsingModuleRuntime()) {
                Method mainMethod = mainClass.getMethod("launch", new Class[]{RuntimeConfig.class, ClassLoader.class, String[].class});
                mainMethod.invoke(null, new Object[]{runtimeConfig, classLoader, args});
            } else {
                Method mainMethod = mainClass.getMethod("main", new Class[]{String[].class});
                mainMethod.invoke(null, new Object[]{args});
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }
    }

    private void trace(String msg) {
        if (runtimeConfig.isDebug()) {
            System.out.println(String.format("[DEBUG] ceres-launcher: %s", msg));
        }
    }
}
