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
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;

/**
 * A layer type is a factory for layer instances and layer (default) configurations.
 * Layer types are managed by the {@link LayerTypeRegistry}.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Marco Zuehlke
 */
public abstract class LayerType extends ExtensibleObject {

    private static final String[] NO_ALIASES = new String[0];

    protected LayerType() {
    }

    /**
     * Gets the name of this layer type. This name is used to persist / externalise layers of this
     * type.
     * <p>
     * The default implementation returns the name given by the
     * {@link com.bc.ceres.glayer.annotations.LayerTypeMetadata#name()} annotation property, if any.
     * Otherwise, the fully qualified class name is returned.
     *
     * <p>
     * As of Ceres 0.13, it is not recommended to override this method. Instead use the
     * {@link com.bc.ceres.glayer.annotations.LayerTypeMetadata#name()}
     * annotation property for your special layer type.
     *
     *
     * @return The name of this layer type.
     */
    public String getName() {
        final LayerTypeMetadata layerTypeMetadata = getClass().getAnnotation(LayerTypeMetadata.class);
        if (layerTypeMetadata != null && !layerTypeMetadata.name().isEmpty()) {
            return layerTypeMetadata.name();
        }
        return getClass().getName();
    }

    /**
     * Gets the alias names under which this layer type is also known. The intention is to allow changing
     * the type name and be backwards compatible with respect to layer types that have been persisted / externalised
     * before the name change occurred.
     * <p>
     * The default implementation returns the alias names given by the
     * {@link com.bc.ceres.glayer.annotations.LayerTypeMetadata#aliasNames()} annotation property, if any.
     * Otherwise, an empty array is returned.
     *
     * <p>
     * As of Ceres 0.13, it is not recommended to override this method. Instead use the {@link LayerTypeMetadata}
     * annotation for your special layer type.
     *
     *
     * @return The aliases of this layer type.
     */
    public String[] getAliases() {
        final LayerTypeMetadata layerTypeMetadata = getClass().getAnnotation(LayerTypeMetadata.class);
        if (layerTypeMetadata != null) {
            return layerTypeMetadata.aliasNames();
        }
        return NO_ALIASES;
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
     * Tests if this type should be created by default with a new ProductSceneView.
     * LayerTypes should check preferences to determine if a user wants this layer type enabled by default when
     * opening a new ProductSceneView.
     *
     * @param ctx An application-dependent layer context.
     * @return {@code true} if the type should open with the scene view by default
     */
    public boolean createWithSceneView(LayerContext ctx) {
        return false;
    }

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
