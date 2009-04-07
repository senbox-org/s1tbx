package org.esa.beam.visat.toolviews.layermanager.layersrc;

import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.assistant.AssistantPageContext;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;

/**
 * Instances of this interface provide the context for implementations of {@link AbstractLayerSourceAssistantPage}.
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
