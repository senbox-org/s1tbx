package org.esa.beam.framework.ui.assistant;

import java.awt.Component;

public abstract class AbstractAssistantPage implements AssistantPage {

    private String pageTitle;
    private Component pageComponent;
    private AssistantPageContext pageContext;

    protected AbstractAssistantPage(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    @Override
    public void setContext(AssistantPageContext pageContext) {
        this.pageContext = pageContext;
    }

    @Override
    public AssistantPageContext getContext() {
        return pageContext;
    }

    @Override
    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    void setPageComponent(Component pageComponent) {
        this.pageComponent = pageComponent;
    }

    @Override
    public final Component getPageComponent() {
        if (pageComponent == null) {
            pageComponent = createPageComponent();
        }
        return pageComponent;
    }

    protected abstract Component createPageComponent();

    @Override
    public AssistantPage getNextPage() {
        return null;
    }

    @Override
    public boolean hasNextPage() {
        return false;
    }

    @Override
    public boolean validatePage() {
        return true;
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    @Override
    public boolean performFinish() {
        return true;
    }

    @Override
    public void performCancel() {
    }

    @Override
    public boolean canHelp() {
        return false;
    }

    @Override
    public void performHelp() {
    }
}
