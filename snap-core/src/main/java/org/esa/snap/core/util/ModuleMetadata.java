package org.esa.snap.core.util;

/**
 * Provides metadata about a module.
 *
 * @author Marco Peters
 */
public interface ModuleMetadata {

    /**
     * A human-readable name used to be shown to a user.
     *
     * @return the display name
     */
    String getDisplayName();

    /**
     * A name which can be used to uniquely identify the module.
     *
     * @return the symbolic name
     */
    String getSymbolicName();

    /**
     * The version of the module.
     *
     * @return the version
     */
    String getVersion();

}
