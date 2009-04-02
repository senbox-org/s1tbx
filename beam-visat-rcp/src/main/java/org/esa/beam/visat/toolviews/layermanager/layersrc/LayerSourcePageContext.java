package org.esa.beam.visat.toolviews.layermanager.layersrc;

import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.assistant.AssistantPageContext;


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
     * Gets the value for the given key.
     *
     * @param key The key of the property.
     *
     * @return The value of the property.
     */
    Object getPropertyValue(String key);

    /**
     * Swets the value for the given key.
     *
     * @param key   The key of the property.
     * @param value The value of the property.
     */
    void setPropertyValue(String key, Object value);
}
