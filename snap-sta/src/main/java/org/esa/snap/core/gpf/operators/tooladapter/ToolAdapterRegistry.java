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
package org.esa.snap.core.gpf.operators.tooladapter;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ToolAdapterOperatorDescriptor;

import java.util.*;
import java.util.stream.Collectors;

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
    private final Set<ToolAdapterListener> listeners;

    ToolAdapterRegistry() {
        registeredAdapters = new HashMap<>();
        listeners = new HashSet<>();
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
        String operatorAlias = operatorDescriptor.getAlias() != null ? operatorDescriptor.getAlias() : operatorDescriptor.getName();
        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi gpfOp = operatorSpiRegistry.getOperatorSpi(operatorAlias);
        if (gpfOp != null) {
            operatorSpiRegistry.removeOperatorSpi(gpfOp);
        }
        operatorSpiRegistry.addOperatorSpi(operatorAlias, operatorSpi);
        registeredAdapters.put(operatorAlias, operatorSpi);
        notifyAdapterAdded((ToolAdapterOperatorDescriptor) operatorSpi.getOperatorDescriptor());
    }

    /**
     * De-registers a ToolAdapterOpSpi given the adapter descriptor.
     *
     * @param operatorDescriptor    The descriptor of the operator to be removed
     */
    public void removeOperator(ToolAdapterOperatorDescriptor operatorDescriptor) {
        String operatorDescriptorName = operatorDescriptor.getName();
        if (registeredAdapters.containsKey(operatorDescriptorName)) {
            registeredAdapters.remove(operatorDescriptorName);
        }
        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi spi = operatorSpiRegistry.getOperatorSpi(operatorDescriptor.getAlias());
        if (spi == null) {
            spi = operatorSpiRegistry.getOperatorSpi(operatorDescriptorName);
        }
        if (spi != null) {
            operatorSpiRegistry.removeOperatorSpi(spi);
            notifyAdapterRemoved(operatorDescriptor);
        }
    }

    public ToolAdapterOperatorDescriptor findByAlias(String alias) {
        ToolAdapterOperatorDescriptor result = null;
        if (alias != null) {
            List<ToolAdapterOpSpi> filtered = registeredAdapters.values().stream()
                    .filter(d -> alias.equals(d.getOperatorAlias()) || alias.equals(d.getOperatorDescriptor().getName()))
                    .collect(Collectors.toList());
            if (filtered.size() > 0) {
                OperatorDescriptor operatorDescriptor = filtered.get(0).getOperatorDescriptor();
                if (operatorDescriptor instanceof ToolAdapterOperatorDescriptor) {
                    result = (ToolAdapterOperatorDescriptor) operatorDescriptor;
                }
            }
        }
        return result;
    }

    void clear() {
        registeredAdapters.clear();
    }

    public void addListener(ToolAdapterListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(ToolAdapterListener listener) {
        this.listeners.remove(listener);
    }

    private void notifyAdapterAdded(final ToolAdapterOperatorDescriptor adapterOperatorDescriptor) {
        if (adapterOperatorDescriptor == null)
            return;
        for (ToolAdapterListener listener : listeners) {
            listener.adapterAdded(adapterOperatorDescriptor);
        }
    }

    private void notifyAdapterRemoved(final ToolAdapterOperatorDescriptor adapterOperatorDescriptor) {
        if (adapterOperatorDescriptor == null)
            return;
        for (ToolAdapterListener listener : listeners) {
            listener.adapterRemoved(adapterOperatorDescriptor);
        }
    }
}
