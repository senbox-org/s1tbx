package org.esa.beam.framework.ui.assistant;

import java.awt.Component;

public abstract class AbstractAppAssistantPage extends AbstractAssistantPage {

    protected AbstractAppAssistantPage(String pageTitle) {
        super(pageTitle);
    }

    public final AppAssistantPageContext getAppPageContext() {
        return (AppAssistantPageContext) getPageContext();
    }

    @Override
    public final AssistantPage getNextPage(AssistantPageContext pageContext) {
        return getNextPage(getAppPageContext());
    }

    public AbstractAppAssistantPage getNextPage(AppAssistantPageContext pageContext) {
        return null;
    }

    @Override
    protected final Component createPageComponent(AssistantPageContext context) {
        return createLayerPageComponent((AppAssistantPageContext) context);
    }

    protected abstract Component createLayerPageComponent(AppAssistantPageContext context);
}
