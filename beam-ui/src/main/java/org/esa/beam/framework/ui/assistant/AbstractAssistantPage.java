package org.esa.beam.framework.ui.assistant;

import java.awt.Component;

/**
 * An abstract implementation of  {@link AssistantPage}.
 * <p>
 * The user has only to implement the {@link #createPageComponent()} method.
 * Other methods have meaningful default implementations, which can be overriden
 * according to the concrete implementation.
 * </p>
 */
public abstract class AbstractAssistantPage implements AssistantPage {

    private String pageTitle;
    private Component pageComponent;
    private AssistantPageContext pageContext;

    /**
     * Creates a new instance with the given page title.
     *
     * @param pageTitle The title of the page.
     */
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

    @Override
    public final Component getPageComponent() {
        if (pageComponent == null) {
            pageComponent = createPageComponent();
        }
        return pageComponent;
    }

    /**
     * Creates the component of this page.
     *
     * @return The component of this page
     */
    protected abstract Component createPageComponent();

    @Override
    public boolean validatePage() {
        return true;
    }

    @Override
    public boolean hasNextPage() {
        return false;
    }

    @Override
    public AssistantPage getNextPage() {
        return null;
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
