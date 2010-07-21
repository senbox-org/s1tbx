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

package com.bc.ceres.glayer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ExtensibleObject;

/**
 * A layer type is a factory for layer instances and layer (default) configurations.
 * Layer types are managed by the {@link LayerTypeRegistry}.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Marco Zuehlke
 */
public abstract class LayerType extends ExtensibleObject {

    private static final String[] ALIASES = new String[0];

    protected LayerType() {
    }
    
    /**
     * Return the name of this layer type. This name will used for storing layer of this
     * type in session.
     * 
     * @return The name of this layer type.
     */
    public String getName() {
        return getClass().getName();
    }
    
    /**
     * Return aliases under which this layer type is also know. the intention is to allow changing
     * the type name and be backwards compatible in regards to old sessions.
     * 
     * @return The aliases of this layer type.
     */
    public String[] getAliases() {
        return ALIASES;
    }

    /**
     * Tests if this type can create layers for the given application provided context.
     * Note that some applications may provide their context through the extension object interface
     * (see {@link #getExtension(Class)}).
     *
     * @param ctx An application-dependent layer context.
     * @return {@code true} if the type is valid with respect to the given context.
     */
    public abstract boolean isValidFor(LayerContext ctx);

    /**
     * Creates a layer instance for the given application provided context and the given layer configuration.
     * The configuration may contain both, inmutable construction parameters passed to specific layer constructor
     * as well as mutable layer properties.
     *
     * @param ctx         An application provided context, may be {@code null}. The parameter may be ignored by many layer types.
     * @param layerConfig The layer configuration.
     * @return A new layer instance.
     */
    public abstract Layer createLayer(LayerContext ctx, PropertySet layerConfig);

    /**
     * Creates a default configuration instance for the type of layers this type can create.
     * After a default configuration has been created it is usually modified to specify a layer's
     * construction parameters, e.g. for an image layer this could be the file path to the image file.
     * Then, an application will pass the configuration to the {@link #createLayer} method in order
     * to create a new layer instance.
     *
     * @param ctx An application provided context, may be {@code null}. The parameter may be ignored by many layer types.
     * @return A new layer (default) configuration.
     */
    public abstract PropertySet createLayerConfig(LayerContext ctx);
}
