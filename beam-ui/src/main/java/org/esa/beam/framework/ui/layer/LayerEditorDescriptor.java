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

package org.esa.beam.framework.ui.layer;

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;

/**
 * A descriptor for a layer editor.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public interface LayerEditorDescriptor {
    /**
     * Gets the {@link Layer} class which is associated directly with a {@link LayerEditor} class or indirectly
     * via a {@link ExtensionFactory} class.
     *
     * @return The {@link LayerType} class. May be {@code null}, if the {@link LayerType} class is given.
     */
    Class<? extends Layer> getLayerClass();

    /**
     * Gets the {@link LayerType} class which is associated directly with a {@link LayerEditor} class or indirectly
     * via a {@link ExtensionFactory} class.
     *
     * @return The {@link LayerType} class. May be {@code null}, if the {@link Layer} class is given.
     */
    Class<? extends LayerType> getLayerTypeClass();

    /**
     * Gets the {@link LayerEditor} class, whose instances serve as suitable editor either
     * for instances of the {@link Layer} class returned by {@link #getLayerClass()} or
     * for instances of the {@link LayerType} class returned by {@link #getLayerTypeClass()}.
     *
     * @return The {@link LayerEditor} class. May be {@code null}, if the {@code ExtensionFactory} class is given.
     */
    Class<? extends LayerEditor> getLayerEditorClass();

    /**
     * Gets the {@link ExtensionFactory} class, whose instances serve as suitable editor factory either
     * for instances of the {@link Layer} class returned by {@link #getLayerClass()} or
     * for instances of the {@link LayerType} class returned by {@link #getLayerTypeClass()}.
     *
     * @return The {@link LayerEditor} class. May be {@code null}, if the {@code LayerEditor} class is given.
     */
    Class<? extends ExtensionFactory> getLayerEditorFactoryClass();
}
