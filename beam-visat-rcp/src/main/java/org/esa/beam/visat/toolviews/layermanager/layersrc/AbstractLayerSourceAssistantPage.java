package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.framework.ui.assistant.AbstractAssistantPage;

public abstract class AbstractLayerSourceAssistantPage extends AbstractAssistantPage {

    protected AbstractLayerSourceAssistantPage(String pageTitle) {
        super(pageTitle);
    }

    @Override
    public LayerSourcePageContext getContext() {
        return (LayerSourcePageContext) super.getContext();
    }
}
