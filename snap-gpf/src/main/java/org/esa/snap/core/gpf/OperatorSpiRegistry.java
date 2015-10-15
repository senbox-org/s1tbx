/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf;

import com.bc.ceres.core.ServiceRegistry;

import java.util.Set;

/**
 * A registry for operator SPI instances.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Marco Zühlke
 * @since 4.1
 */
public interface OperatorSpiRegistry {

    /**
     * Loads the SPIs defined in {@code META-INF/services}.
     */
    void loadOperatorSpis();

    /**
     * @return The set of all registered operator SPIs.
     * @since BEAM 5
     */
    Set<OperatorSpi> getOperatorSpis();

    /**
     * Gets the {@link ServiceRegistry ServiceRegistry}
     *
     * @return the {@link ServiceRegistry service registry}
     */
    ServiceRegistry<OperatorSpi> getServiceRegistry();

    /**
     * Gets a registered operator SPI. The given <code>operatorName</code> can be
     * either the fully qualified class name of the {@link OperatorSpi}
     * or an alias name.
     *
     * @param operatorName a name identifying the operator SPI.
     *
     * @return the operator SPI, or <code>null</code>
     */
    OperatorSpi getOperatorSpi(String operatorName);

    /**
     * Adds the given {@link OperatorSpi operatorSpi} to this registry.
     *
     * @param operatorSpi the SPI to add
     *
     * @return {@code true}, if the {@link OperatorSpi} could be successfully added, otherwise {@code false}
     */
    boolean addOperatorSpi(OperatorSpi operatorSpi);

    /**
     * Adds the given {@link OperatorSpi operatorSpi} to this registry.
     *
     * @param operatorName an (alias) name used as key for the registration.
     * @param operatorSpi the SPI to add
     *
     * @return {@code true}, if the {@link OperatorSpi} could be successfully added, otherwise {@code false}
     * @since BEAM 5
     */
    boolean addOperatorSpi(String operatorName, OperatorSpi operatorSpi);

    /**
     * Removes the given {@link OperatorSpi operatorSpi} this registry.
     *
     * @param operatorSpi the SPI to remove
     *
     * @return {@code true}, if the SPI could be removed, otherwise {@code false}
     */
    boolean removeOperatorSpi(OperatorSpi operatorSpi);

    /**
     * Sets an alias for the given SPI class name.
     *
     * @param aliasName    the alias
     * @param spiClassName the name of the SPI class
     * @deprecated since BEAM 5, used internally only
     */
    @Deprecated
    void setAlias(String aliasName, String spiClassName);

    /**
     *  Gets a set of all aliases
     *
     * @return the Set&lt;string&gt; of alias keys
     */
    public Set getAliases();
}
