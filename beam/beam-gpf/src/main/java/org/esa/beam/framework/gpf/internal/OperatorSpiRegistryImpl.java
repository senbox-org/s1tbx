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
package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import com.bc.ceres.core.ServiceRegistryListener;
import org.esa.beam.BeamCoreActivator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A registry for operator SPI instances.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @since 4.1
 */
public class OperatorSpiRegistryImpl implements OperatorSpiRegistry {

    private final ServiceRegistry<OperatorSpi> serviceRegistry;
    private final Map<String, String> aliases;

    /**
     * The provate singleton constructor.
     */
    public OperatorSpiRegistryImpl() {
        serviceRegistry = ServiceRegistryManager.getInstance().getServiceRegistry(OperatorSpi.class);
        aliases = new HashMap<String, String>(20);
        serviceRegistry.addListener(new ServiceRegistryListener<OperatorSpi>() {
            public void serviceAdded(ServiceRegistry<OperatorSpi> registry, OperatorSpi service) {
                setAliases(service);
            }

            public void serviceRemoved(ServiceRegistry<OperatorSpi> registry, OperatorSpi service) {
                unregisterAliases(service);
            }
        });
        Set<OperatorSpi> services = serviceRegistry.getServices();
        for (OperatorSpi operatorSpi : services) {
            setAliases(operatorSpi);
        }
    }

    /**
     * Loads the SPI's defined in {@code META-INF/services}.
     */
    public void loadOperatorSpis() {
        if (!BeamCoreActivator.isStarted()) {
            BeamCoreActivator.loadServices(getServiceRegistry());
        }
    }

    /**
     * Gets the {@link ServiceRegistry ServiceRegistry}
     *
     * @return the {@link ServiceRegistry service registry}
     */
    public ServiceRegistry<OperatorSpi> getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Gets a registrered operator SPI. The given <code>operatorName</code> can be
     * either the fully qualified class name of the {@link OperatorSpi}
     * or an alias name.
     *
     * @param operatorName A name identifying the operator SPI.
     * @return the operator SPI, or <code>null</code>
     */
    public OperatorSpi getOperatorSpi(String operatorName) {
        OperatorSpi service = serviceRegistry.getService(operatorName);
        if (service != null) {
            return service;
        }
        operatorName = aliases.get(operatorName);
        if (operatorName != null) {
            service = serviceRegistry.getService(operatorName);
        }
        return service;
    }

    /**
     * Adds the given {@link OperatorSpi operatorSpi} to this registry.
     *
     * @param operatorSpi the SPI to add
     * @return {@code true}, if the {@link OperatorSpi} could be succesfully added, otherwise {@code false}
     */
    public boolean addOperatorSpi(OperatorSpi operatorSpi) {
        Class<? extends OperatorSpi> spiClass = operatorSpi.getClass();
        String spiClassName = spiClass.getName();
        if (serviceRegistry.getService(spiClassName) == operatorSpi) {
            return false;
        }
        setAliases(operatorSpi);
        return serviceRegistry.addService(operatorSpi);
    }

    /**
     * Removes the given {@link OperatorSpi operatorSpi} this registry.
     *
     * @param operatorSpi the SPI to remove
     * @return {@code true}, if the SPI could be removed, otherwise {@code false}
     */
    public boolean removeOperatorSpi(OperatorSpi operatorSpi) {
        return serviceRegistry.removeService(operatorSpi);
    }

    /**
     * Sets an alias for the given SPI class name.
     *
     * @param aliasName    the alias
     * @param spiClassName the name of the SPI class
     */
    public void setAlias(String aliasName, String spiClassName) {
        aliases.put(aliasName, spiClassName);
    }

    private void setAliases(OperatorSpi operatorSpi) {
        Class<? extends OperatorSpi> spiClass = operatorSpi.getClass();
        setAlias(operatorSpi.getOperatorAlias(), spiClass.getName());
    }

    /**
     *  Gets a set of all aliases
     *
     * @return the Set<string> of keys
     */
	// NESTMOD
    public Set getAliases() {
        return aliases.keySet();
    }

    private void unregisterAliases(OperatorSpi operatorSpi) {
        Class<? extends OperatorSpi> spiClass = operatorSpi.getClass();
        String spiClassName = spiClass.getName();
        String[] keys = aliases.keySet().toArray(new String[0]);
        for (String key : keys) {
            if (aliases.get(key).equalsIgnoreCase(spiClassName)) {
                aliases.remove(key);
            }
        }
    }

}
