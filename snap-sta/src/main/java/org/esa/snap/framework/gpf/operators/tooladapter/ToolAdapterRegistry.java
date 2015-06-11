/*
 * Copyright (C) 2014-2015 CS SI
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
 *  with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.framework.gpf.operators.tooladapter;

import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.OperatorSpiRegistry;
import org.esa.snap.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.framework.gpf.descriptor.ToolAdapterOperatorDescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registry (map) class for mapping ToolAdapterOpSpi-s to adapter names.
 *
 * @author Cosmin Cara
 */
public enum ToolAdapterRegistry {
    /**
     * The singleton instance of the class
     */
    INSTANCE;

    private final Map<String, ToolAdapterOpSpi> registeredAdapters;

    ToolAdapterRegistry() {
        registeredAdapters = new HashMap<>();
    }

    /**
     * Gets the registered ToolAdapterOpSpi-s
     *
     * @return  the map of the registered ToolAdapterOpSpi-s
     */
    public Map<String, ToolAdapterOpSpi> getOperatorMap() {
        return registeredAdapters;
    }

    /**
     * Adds an operator to this registry and registers it in the global
     * OperatorSpiRegistry.
     *
     * @param operatorSpi   The SPI to be registered
     */
    public void registerOperator(ToolAdapterOpSpi operatorSpi) {
        OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();
        String operatorName = operatorDescriptor.getName() != null ? operatorDescriptor.getName() : operatorDescriptor.getAlias();
        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        if (operatorSpiRegistry.getOperatorSpi(operatorName) == null) {
            operatorSpiRegistry.addOperatorSpi(operatorName, operatorSpi);
        }
        if (registeredAdapters.containsKey(operatorName)) {
            registeredAdapters.remove(operatorName);
            registeredAdapters.put(operatorName, operatorSpi);
            Logger.getGlobal().warning(String.format("An operator with the name %s was already registered", operatorName));
        }
        registeredAdapters.put(operatorName, operatorSpi);
    }

    /**
     * De-registers a ToolAdapterOpSpi given the adapter descriptor.
     *
     * @param operatorDescriptor    The descriptor of the operator to be removed
     */
    public void removeOperator(ToolAdapterOperatorDescriptor operatorDescriptor) {
        if (!operatorDescriptor.isSystem()) {
            String operatorDescriptorName = operatorDescriptor.getName();
            if (registeredAdapters.containsKey(operatorDescriptorName)) {
                registeredAdapters.remove(operatorDescriptorName);
            }
            OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
            OperatorSpi spi = operatorSpiRegistry.getOperatorSpi(operatorDescriptor.getName());
            if (spi != null) {
                operatorSpiRegistry.removeOperatorSpi(spi);
            }
        }
    }

    void clear() {
        registeredAdapters.clear();
    }
}
