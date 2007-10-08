package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ServiceRegistry;

/**
 * A registry for operator SPI instances.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @since 4.1
 */
public interface OperatorSpiRegistry {
    /**
     * Loads the SPI's defined in {@code META-INF/services}.
     */
    void loadOperatorSpis();

    /**
     * Gets the {@link ServiceRegistry ServiceRegistry}
     *
     * @return the {@link ServiceRegistry service registry}
     */
    ServiceRegistry<OperatorSpi> getServiceRegistry();

    /**
     * Gets a registrered operator SPI. The given <code>operatorName</code> can be
     * either the fully qualified class name of the {@link OperatorSpi}
     * or an alias name.
     *
     * @param operatorName a name identifying the operator SPI.
     * @return the operator SPI, or <code>null</code>
     */
    OperatorSpi getOperatorSpi(String operatorName);

    /**
     * Adds the given {@link OperatorSpi operatorSpi} to this registry.
     *
     * @param operatorSpi the SPI to add
     * @return {@code true}, if the {@link OperatorSpi} could be succesfully added, otherwise {@code false}
     */
    boolean addOperatorSpi(OperatorSpi operatorSpi);

    /**
     * Removes the given {@link OperatorSpi operatorSpi} this registry.
     *
     * @param operatorSpi the SPI to remove
     * @return {@code true}, if the SPI could be removed, otherwise {@code false}
     */
    boolean removeOperatorSpi(OperatorSpi operatorSpi);

    /**
     * Sets an alias for the given SPI class name.
     *
     * @param aliasName    the alias
     * @param spiClassName the name of the SPI class
     */
    void setAlias(String aliasName, String spiClassName);
}
