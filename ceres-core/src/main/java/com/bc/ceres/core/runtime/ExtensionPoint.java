package com.bc.ceres.core.runtime;

/**
 * An extension point declared in a module.
 * <p/>
 * <p>If {@link #getDeclaringModule() declared} in a module manifest (module.xml), an extension point has the following syntax:
 * <pre>
 *    &lt;extensionPoint id="{@link #getId() id}"&gt;
 *       {@link #getConfigurationShemaElement() configuration shema element 1}
 *       {@link #getConfigurationShemaElement() configuration shema element 2}
 *       ...
 *    &lt;/extensionPoint&gt;
 * </pre>
 * <p/>
 * This interface is not intended to be implemented by clients.</p>
 */
public interface ExtensionPoint {

    /**
     * Gets the identifier.
     *
     * @return The identifier.
     */
    String getId();

    /**
     * Gets the qualified identifier (module identifier plus extension point identifier separated by a colon ':').
     *
     * @return The qualified identifier.
     */
    String getQualifiedId();

    /**
     * Gets the configuration shema element of this extension point.
     *
     * @return The configuration shema element.
     */
    ConfigurationSchemaElement getConfigurationSchemaElement();

    /**
     * Gets the configuration shema element of this extension point.
     *
     * @return The configuration shema element.
     * @deprecated since Ceres 0.10, use {@link #getConfigurationSchemaElement()} instead
     */
    @Deprecated
    ConfigurationShemaElement getConfigurationShemaElement();

    /**
     * Gets the module in which this extension point is declared.
     *
     * @return The declaring module.
     */
    Module getDeclaringModule();

    /**
     * Gets all extensions extending this extension point.
     *
     * @return All extensions, or {@code null} if the declaring module has not yet been registered.
     */
    Extension[] getExtensions();

    /**
     * Gets all configuration elements of all extensions extending this extension point.
     *
     * @return All configuration elements, or {@code null} if the declaring module has not yet been registered.
     */
    ConfigurationElement[] getConfigurationElements();
}

