package org.esa.beam.framework.ui.assistant;

import java.awt.Component;

public abstract class AbstractAppAssistantPage extends AbstractAssistantPage {

    protected AbstractAppAssistantPage(String pageTitle) {
        super(pageTitle);
    }

    @Override
    public final AssistantPage getNextPage(AssistantPageContext pageContext) {
        return getNextPage((AppAssistantPageContext)pageContext);
    }

    public AbstractAppAssistantPage getNextPage(AppAssistantPageContext pageContext) {
        return null;
    }

    @Override
    public boolean performFinish(AssistantPageContext pageContext) {
        return performFinish((AppAssistantPageContext)pageContext);

    }

    public boolean performFinish(AppAssistantPageContext pageContext) {
        return true;
    }

    @Override
    protected final Component createPageComponent(AssistantPageContext context) {
        return createLayerPageComponent((AppAssistantPageContext) context);
    }

    public abstract Component createLayerPageComponent(AppAssistantPageContext context);
}
