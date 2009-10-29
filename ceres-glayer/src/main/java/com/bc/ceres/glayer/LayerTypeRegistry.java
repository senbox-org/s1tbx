/*
 * $Id: $
 * 
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.bc.ceres.glayer;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import com.bc.ceres.core.runtime.internal.RuntimeActivator;

import java.util.ServiceLoader;

/**
 * A registry for layer type instances.
 * <p/>
 * In order to register new layer types, use the standard {@code META-INF/services}
 * JAR service provider interface (SPI). The service priovider name is identical to this
 * class' fully qualified name: {@code com.bc.ceres.glayer.LayerType}.
 *
 * @author Marco Zuehlke
 * @since ceres 0.10
 */
public class LayerTypeRegistry {

    private final ServiceRegistry<LayerType> registry;

    /**
     * Return the instance for the given layerType class name.
     * 
     * @param layerTypeClassName
     *            The class name of the layer type.
     * @return the instance
     */
    public static LayerType getLayerType(String layerTypeClassName) {
        return getRegistry().getService(layerTypeClassName);
    }

    /**
     * Return the instance for the given layerType class.
     * 
     * @param layerTypeClass
     *            The class of the layer type.
     * @return the instance
     */
    public static <T extends LayerType> T getLayerType(Class<T> layerTypeClass) {
        return (T) getRegistry().getService(layerTypeClass.getName());
    }

    private static ServiceRegistry<LayerType> getRegistry() {
        return Holder.instance.registry;
    }

    private LayerTypeRegistry() {
        registry = ServiceRegistryManager.getInstance().getServiceRegistry(LayerType.class);
        if (RuntimeActivator.getInstance() == null) {
            final ServiceLoader<LayerType> serviceLoader = ServiceLoader.load(LayerType.class);
            for (final LayerType layerType : serviceLoader) {
                registry.addService(layerType);
            }
        }
    }

    // Initialization on demand holder idiom, see
    // http://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom
    private static class Holder {
        static LayerTypeRegistry instance = new LayerTypeRegistry();
    }
}
