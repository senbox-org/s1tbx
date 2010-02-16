package com.bc.ceres.core.runtime;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.Extensible;

import java.net.URL;
import java.util.logging.Logger;

/**
 * The context in which a module lives.
 * <p/>
 * This interface is not intended to be implemented by clients.</p>
 */
public interface ModuleContext extends Extensible {
    /**
     * Gets the Ceres runtime configuration.
     * @return the Ceres runtime configuration.
     */
    RuntimeConfig getRuntimeConfig();

    /**
     * @return The module to which this context belongs to.
     */
    Module getModule();

    /**
     * Gets the module for the given module identifier.
     *
     * @param id The module identifier. If zero is passed, the
     *           system module is returned, which is always present.
     *
     * @return A module or <code>null</code> if no such exists.
     */
    Module getModule(long id);

    /**
     * Gets all modules in this context.
     *
     * @return All modules or an empty array.
     */
    Module[] getModules();

    /**
     * @return The context's logger.
     */
    Logger getLogger();

    /**
     * Installs the module from the given URL.
     *
     * @param url the URL
     * @param proxyConfig the proxy configuration, can be null
     * @param pm  the progress monitor
     *
     * @return the new module
     *
     * @throws CoreException if an error occurs
     */
    Module installModule(URL url, ProxyConfig proxyConfig, ProgressMonitor pm) throws CoreException;
}
