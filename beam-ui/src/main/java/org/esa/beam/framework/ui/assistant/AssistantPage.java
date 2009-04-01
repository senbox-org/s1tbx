package org.esa.beam.framework.ui.assistant;

import java.awt.Component;

public interface AssistantPage {

    String getPageTitle();

    Component getPageComponent();

    /**
     * @return true, if  a next page is available
     */
    boolean hasNextPage();

    /**
     * Called only if {@link #validatePage()} returns true.
     *
     * @return the next page, or {@code null} if no next page exists or the page could not be created.
     */
    AssistantPage getNextPage();

    /**
     * Called from {@link AssistantPageContext#updateState()} in order to validate user inputs.
     *
     * @return true, if the current page is valid
     */
    boolean validatePage();

    boolean canFinish();

    boolean performFinish();

    void performCancel();

    boolean canHelp();

    void performHelp();

    void setContext(AssistantPageContext pageContext);

    AssistantPageContext getContext();
}
