package org.esa.beam.framework.ui.layer;

import org.esa.beam.framework.ui.assistant.AbstractAssistantPage;
import org.esa.beam.framework.ui.layer.LayerSource;

/**
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
public abstract class AbstractLayerSourceAssistantPage extends AbstractAssistantPage {

    protected AbstractLayerSourceAssistantPage(String pageTitle) {
        super(pageTitle);
    }

    @Override
    public LayerSourcePageContext getContext() {
        return (LayerSourcePageContext) super.getContext();
    }
    
    @Override
    public void performCancel() {
        LayerSourcePageContext context = getContext();
        LayerSource layerSource = context.getLayerSource();
        if (layerSource != null) {
            layerSource.cancel(context);
        }
    }
}
