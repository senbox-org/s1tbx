package org.esa.beam.framework.ui.assistant;

import java.awt.Component;

public abstract class AbstractAssistantPage implements AssistantPage {

    private String pageTitle;
    private Component pageComponent;

    protected AbstractAssistantPage(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    @Override
    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public Component getPageComponent() {
        return pageComponent;
    }

    void setPageComponent(Component pageComponent) {
        this.pageComponent = pageComponent;
    }

    @Override
    public final Component getPageComponent(AssistantPageContext context) {
        if (pageComponent == null) {
            pageComponent = createPageComponent(context);
        }
        return pageComponent;
    }

    protected abstract Component createPageComponent(AssistantPageContext context);

    @Override
    public AssistantPage getNextPage(AssistantPageContext pageContext) {
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
    public boolean performFinish(AssistantPageContext pageContext) {
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
