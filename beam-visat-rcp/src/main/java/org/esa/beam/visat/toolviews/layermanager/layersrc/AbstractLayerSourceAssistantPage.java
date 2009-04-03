package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.framework.ui.assistant.AbstractAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;

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
