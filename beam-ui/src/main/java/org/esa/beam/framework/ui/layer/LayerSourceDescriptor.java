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
 * The {@code LayerSourceDescriptor} provides metadata and
 * a factory method for a {@link LayerSource}.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $ Revision $ $ Date $
 * @since BEAM 4.6
 */
public interface LayerSourceDescriptor {

    /**
     * A unique ID.
     *
     * @return The unique ID.
     */
    String getId();

    /**
     * A human readable name.
     *
     * @return The name.
     */
    String getName();

    /**
     * A text describing what the {@link LayerSource}, created by
     * this {@code LayerSourceDescriptor}, does.
     *
     * @return A description.
     */
    String getDescription();

    /**
     * Creates the {@link LayerSource} which is used in the graphical user interface to
     * add {@link com.bc.ceres.glayer.Layer} to a view.
     *
     * @return The {@link LayerSource}.
     */
    LayerSource createLayerSource();

    /**
     * The {@link LayerType}.
     *
     * @return the type of the layer which is added to a view, or {@code null} if
     *         multiple layers are added.
     */
    LayerType getLayerType();
}
