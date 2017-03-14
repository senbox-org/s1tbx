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
package org.esa.snap.core.gpf.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryListener;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.util.ServiceLoader;
import org.esa.snap.core.util.SystemUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * A registry for operator SPI instances.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @since 4.1
 */
public class OperatorSpiRegistryImpl implements OperatorSpiRegistry {

    private final ServiceRegistry<OperatorSpi> serviceRegistry;
    private final Map<String, String> classNames;
    private final Map<String, OperatorSpi> extraOperatorSpis;

    /**
     * The constructor.
     *
     * @param serviceRegistry The underlying service registry used by this instance.
     */
    public OperatorSpiRegistryImpl(ServiceRegistry<OperatorSpi> serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        classNames = new ConcurrentHashMap<>(20);
        this.serviceRegistry.addListener(new ServiceRegistryListener<OperatorSpi>() {
            @Override
            public void serviceAdded(ServiceRegistry<OperatorSpi> registry, OperatorSpi service) {
                registerAlias(service);
            }

            @Override
            public void serviceRemoved(ServiceRegistry<OperatorSpi> registry, OperatorSpi service) {
                unregisterAlias(service);
            }
        });
        Set<OperatorSpi> services = this.serviceRegistry.getServices();
        for (OperatorSpi operatorSpi : services) {
            registerAlias(operatorSpi);
        }
        extraOperatorSpis = new ConcurrentHashMap<>();
    }

    /**
     * Loads the SPIs defined in {@code META-INF/services}.
     */
    @Override
    public void loadOperatorSpis() {
        ServiceLoader.loadServices(getServiceRegistry());
    }

    /**
     * @return The set of all registered operator SPIs.
     * @since BEAM 5
     */
    @Override
    public Set<OperatorSpi> getOperatorSpis() {
        HashSet<OperatorSpi> operatorSpis = new HashSet<>(serviceRegistry.getServices());
        operatorSpis.addAll(extraOperatorSpis.values());
        return operatorSpis;
    }

    /**
     * Gets the {@link ServiceRegistry ServiceRegistry}
     *
     * @return the {@link ServiceRegistry service registry}
     */
    @Override
    public ServiceRegistry<OperatorSpi> getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Gets a registered operator SPI. The given {@code operatorName} can be
     * either the fully qualified class name of the {@link OperatorSpi}
     * or an alias name.
     *
     * @param operatorName A name identifying the operator SPI.
     * @return the operator SPI, or {@code null}
     */
    @Override
    public OperatorSpi getOperatorSpi(String operatorName) {
        OperatorSpi service = serviceRegistry.getService(operatorName);
        if (service != null) {
            return service;
        }

        service = getName(extraOperatorSpis, operatorName);
        if (service != null) {
            return service;
        }

        String className = getName(classNames, operatorName);
        if (className != null) {
            service = serviceRegistry.getService(className);
            if (service != null) {
                return service;
            }
        }

        return null;
    }

    private <T> T getName(Map<String, T> tMap, String operatorName) {
        Optional<String> optional = tMap.keySet().stream().filter(p -> p.equalsIgnoreCase(operatorName)).findFirst();
        return optional.isPresent() ? tMap.get(optional.get()) : null;
    }

    /**
     * Adds the given {@link OperatorSpi operatorSpi} to this registry.
     *
     * @param operatorSpi the SPI to add
     * @return {@code true}, if the {@link OperatorSpi} could be successfully added, otherwise {@code false}
     */
    @Override
    public boolean addOperatorSpi(OperatorSpi operatorSpi) {
        String spiClassName = operatorSpi.getClass().getName();
        if (serviceRegistry.getService(spiClassName) == operatorSpi) {
            return false;
        }
        registerAlias(operatorSpi);
        return serviceRegistry.addService(operatorSpi);
    }

    /**
     * Adds the given {@link OperatorSpi operatorSpi} to this registry.
     *
     * @param operatorName an (alias) name used as key for the registration.
     * @param operatorSpi  the SPI to add
     * @return {@code true}, if the {@link OperatorSpi} could be successfully added, otherwise {@code false}
     * @since BEAM 5
     */
    @Override
    public boolean addOperatorSpi(String operatorName, OperatorSpi operatorSpi) {
        if (operatorName.equals(operatorSpi.getClass().getName())) {
            return addOperatorSpi(operatorSpi);
        }
        if (extraOperatorSpis.get(operatorName) == operatorSpi) {
            return false;
        }
        registerAlias(operatorSpi.getClass().getName(), operatorName);
        extraOperatorSpis.put(operatorName, operatorSpi);
        return true;
    }

    /**
     * Removes the given {@link OperatorSpi operatorSpi} this registry.
     *
     * @param operatorSpi the SPI to remove
     * @return {@code true}, if the SPI could be removed, otherwise {@code false}
     */
    @Override
    public boolean removeOperatorSpi(OperatorSpi operatorSpi) {
        if (!serviceRegistry.removeService(operatorSpi)) {
            Stream<Map.Entry<String, OperatorSpi>> extraSpiStream = extraOperatorSpis.entrySet().stream();
            Optional<Map.Entry<String, OperatorSpi>> spiEntry = extraSpiStream.filter(entry -> entry.getValue() == operatorSpi).findFirst();
            if (spiEntry.isPresent() && extraOperatorSpis.remove(spiEntry.get().getKey(), spiEntry.get().getValue())) {
                unregisterAlias(spiEntry.get().getValue());
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the set of all aliases.
     *
     * @return the set of alias names.
     */
    public Set<String> getAliases() {
        return classNames.keySet();
    }

    /**
     * Sets an alias for the given SPI class name.
     *
     * @param aliasName    the alias
     * @param spiClassName the name of the SPI class
     * @deprecated since BEAM 5, used internally only
     */
    @Deprecated
    @Override
    public void setAlias(String aliasName, String spiClassName) {
        registerAlias(spiClassName, aliasName);
    }

    private void registerAlias(String spiClassName, String aliasName) {
        Assert.notNull(aliasName, "aliasName");
        Assert.notNull(spiClassName, "spiClassName");
        if (classNames.get(aliasName) != null) {
            SystemUtils.LOG.severe(
                    spiClassName + ':' + aliasName + " conflicts with " + classNames.get(aliasName) + ':' + aliasName);
        }
        classNames.put(aliasName, spiClassName);
    }

    private void registerAlias(OperatorSpi operatorSpi) {
        String operatorAlias = operatorSpi.getOperatorDescriptor().getAlias();
        if (operatorAlias != null) {
            registerAlias(operatorSpi.getClass().getName(), operatorAlias);
        }
    }

    private void unregisterAlias(OperatorSpi operatorSpi) {
        String alias = operatorSpi.getOperatorAlias();
        if (classNames.remove(alias) == null) {
            String spiClassName = operatorSpi.getClass().getName();
            Stream<String> stream = new HashSet<>(classNames.keySet()).stream();
            stream.filter(key -> classNames.get(key).equals(spiClassName)).forEach(classNames::remove);
        }
    }

}
