package org.esa.beam.framework.ui.mpage;

import java.awt.Component;

public abstract class AbstractPage implements Page {
    private String pageTitle;
    private Component pageComponent;
    private PageContext context;

    public AbstractPage(String pageTitle) {
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

    public final PageContext getPageContext() {
        return context;
    }

    public final Component getPageComponent(PageContext context) {
        this.context = context;
        if (pageComponent == null) {
            pageComponent = createPageComponent(context);
        }
        return pageComponent;
    }

    protected abstract Component createPageComponent(PageContext context);

    public Page getNextPage() {
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
