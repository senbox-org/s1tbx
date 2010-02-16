package com.bc.ceres.launcher;

import com.bc.ceres.core.runtime.RuntimeConfigException;

import java.net.URL;

/**
 * Used to construct the classpath used by the launcher.
 * May be implemented by clients in order to construct a custom classpath.
 */
public interface ClasspathFactory {
    /**
     * Creates the classpath.
     *
     * @return the classpath which may be empty.
     * @throws com.bc.ceres.core.runtime.RuntimeConfigException
     *          If the configuration causes problems.
     */
    URL[] createClasspath() throws RuntimeConfigException;
}
