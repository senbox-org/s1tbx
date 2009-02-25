package org.esa.beam.framework.ui.assistant;

import java.awt.Component;

public abstract class AbstractAssistantPage implements AssistantPage {
    private String pageTitle;
    private Component pageComponent;
    private AssistantPageContext context;

    public AbstractAssistantPage(String pageTitle) {
        this.pageTitle = pageTitle;
    }

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

    public final AssistantPageContext getPageContext() {
        return context;
    }

    public final Component getPageComponent(AssistantPageContext context) {
        this.context = context;
        if (pageComponent == null) {
            pageComponent = createPageComponent(context);
        }
        return pageComponent;
    }

    protected abstract Component createPageComponent(AssistantPageContext context);

    public AssistantPage getNextPage() {
        return null;
    }

    public boolean hasNextPage() {
        return false;
    }

    public boolean validatePage() {
        return true;
    }

    public boolean canFinish() {
        return true;
    }

    public boolean performFinish() {
        return true;
    }

    public void performCancel() {
    }

    public boolean canHelp() {
        return false;
    }

    public void performHelp() {
    }
}
