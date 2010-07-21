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

import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.layer.LayerSource;
import org.esa.beam.framework.ui.assistant.AssistantPageContext;

/**
 * Instances of this interface provide the context for implementations of {@link AbstractLayerSourceAssistantPage}.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
public interface LayerSourcePageContext extends AssistantPageContext {

    /**
     * Gets the {@link AppContext} of the application.
     *
     * @return The {@link AppContext} of the application.
     */
    AppContext getAppContext();

    /**
     * Gets the {@link LayerContext layer context} of the selected view.
     *
     * @return The {@link LayerContext layer context} of the selected view.
     */
    LayerContext getLayerContext();

    /**
     * Sets the {@link LayerSource} of this assistant.
     *
     * @param layerSource The {@link LayerSource} of this assistant.
     */
    void setLayerSource(LayerSource layerSource);

    /**
     * Gets the {@link LayerSource} of this assistant.
     *
     * @return The {@link LayerSource} of this assistant.
     */
    LayerSource getLayerSource();

    /**
     * Gets the value for the given key.
     *
     * @param key The key of the property.
     *
     * @return The value of the property.
     */
    Object getPropertyValue(String key);

    /**
     * Sets the value for the given key.
     *
     * @param key   The key of the property.
     * @param value The value of the property.
     */
    void setPropertyValue(String key, Object value);
}
