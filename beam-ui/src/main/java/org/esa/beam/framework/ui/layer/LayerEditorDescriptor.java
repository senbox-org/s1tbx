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
     * Gets the {@link LayerType} class for which the {@link LayerEditor} is intended.
     * The corresponding {@code LayerEditor} class can be retrieved by a call to
     * {@link #getLayerEditorClass()}.
     *
     * @return The {@link LayerType} class.
     */
    Class<? extends LayerType> getLayerTypeClass();

    /**
     * Gets the {@link LayerEditor} class, which is intended for the
     * {@link LayerType} class returned by {@link #getLayerTypeClass()}
     *
     * @return The {@link LayerEditor} class
     */
    Class<? extends LayerEditor> getLayerEditorClass();
}
