package com.bc.ceres.core.runtime;

/**
 * An extension declared in a module.
 * <p/>
 * <p>If {@link #getDeclaringModule() declared} in a module manifest (module.xml), an extension has the following syntax:
 * <pre>
 *    &lt;extension point="{@link #getPoint() point}"&gt;
 *       {@link #getConfigurationElement() configuration element 1}
 *       {@link #getConfigurationElement() configuration element 2}
 *       ...
 *    &lt;/extension&gt;
 * </pre>
 * </p>
 * <p/>
 * <p>An extension can also have an optional identifier which makes it possible to retrieve it via the
 * {@link Module#getExtension(String)} method:
 * <pre>
 *    &lt;extension id="{@link #getId() id}" point="{@link #getPoint() point}"&gt;
 *       ...
 *    &lt;/extension&gt;
 * </pre>
 * <p/>
 * This interface is not intended to be implemented by clients.</p>
 */
public interface Extension {

    /**
     * Gets the name of the extension point which is extended by this extension.
     *
     * @return The name of the extension point.
     */
    String getPoint();

    /**
     * Gets the (optional) identifier.
     *
     * @return The identifier, can be {@code null}.
     */
    String getId();

    /**
     * Gets the configuration element of this extension.
     *
     * @return The configuration element.
     */
    ConfigurationElement getConfigurationElement();

    /**
     * Gets the module in which this extension is declared.
     *
     * @return The declaring module.
     */
    Module getDeclaringModule();

    /**
     * Gets the extension point which is extended by this extension.
     *
     * @return The extension point or {@code null} if the declaring module is yet neither registered nor resolved.
     */
    ExtensionPoint getExtensionPoint();
}
