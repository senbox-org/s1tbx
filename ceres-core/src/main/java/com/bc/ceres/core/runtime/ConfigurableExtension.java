package com.bc.ceres.core.runtime;

import com.bc.ceres.core.CoreException;

/**
 * A configurable extension.
 * This interface may be implemented by client supplied extensions.</p>
 *
 * @see ConfigurationElement#createExecutableExtension(Class<T>)
 */
public interface ConfigurableExtension {

    /**
     * Configures this extension with the supplied configuration data.
     *
     * @param config The configuration data.
     *
     * @throws CoreException if an error occured during configuration.
     */
    void configure(ConfigurationElement config) throws CoreException;
}
