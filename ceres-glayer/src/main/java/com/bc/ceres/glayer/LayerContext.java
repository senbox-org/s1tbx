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

/**
 * The context in which layers are managed, created and/or rendered, e.g. the view used to
 * display multiple layers.
 * <p>
 * By default, the context is composed of the root layer and a common coordinate reference system (CRS)
 * shared by all layers, this is the root layer and all of its child layers and so forth.
 * <p>
 * Instances of this interface are passed to the several methods of {@link LayerType}
 * in order to provide special layer type implementations with access to application specific services.
 * Therefore this interface is intended to be implemented by clients.
 * Since implementations of this interface are application-specific, there is no default implementation.
 *
 * @author Norman Fomferra
 */
public interface LayerContext {
    /**
     * The coordinate reference system (CRS) used by all the layers in this context.
     * The CRS defines the model coordinate system and may be used by a
     * {@link com.bc.ceres.glayer.LayerType} in order to decide whether
     * it is can create new layer instance for this context.
     *
     * @return The CRS. May be {@code null}, if not used.
     */
    Object getCoordinateReferenceSystem();

    /**
     * @return The root layer.
     */
    Layer getRootLayer();
}
