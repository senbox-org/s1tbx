package com.bc.ceres.launcher;

import com.bc.ceres.core.runtime.RuntimeConfigException;

import java.net.URL;

/**
 * Used to construct the classpath used by the launcher.
 */
public interface ClasspathFactory {
    /**
     * Creates the classpath.
     * @return the classpath which may be empty.
     */
    URL[] createClasspath() throws RuntimeConfigException;
}
