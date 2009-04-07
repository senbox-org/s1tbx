package org.esa.beam.framework.ui.assistant;

import java.awt.Component;

/**
 * An {@code AssistantPage} is part of a sequence of pages. <br/>
 * It is used by an {@link AssistantPane} to assists the user to
 * accomplish a specific task by stepping through the pages.
 */
public interface AssistantPage {

    /**
     * Gets the title of the page.
     *
     * @return The page title.
     */
    String getPageTitle();

    /**
     * Gets the component of the page.
     *
     * @return The page component.
     */
    Component getPageComponent();

    /**
     * @return true, if  a next page is available
     */
    boolean hasNextPage();

    /**
     * Called only if {@link #validatePage()} and {@link #hasNextPage()}  return {@code true}.
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

    /**
     * Determines if the page can finish the current assitant or not.
     *
     * @return {@code true} if the page can perform finish, otherwise {@code false}
     */
    boolean canFinish();

    /**
     * Ccalled only if {@link #validatePage()} and {@link #canFinish()} return {@code true}.
     *
     * @return {@code true} if finishing was successful, otherwise {@code false}.
     */
    boolean performFinish();

    /**
     * Cancels the current execution of the assitant.
     * Implementors shall release allocated resources.
     */
    void performCancel();

    /**
     * Determines if the page can show help information.
     *
     * @return {@code true} if the page can show help information, otherwise {@code false}
     */
    boolean canHelp();

    /**
     * Only called if @link #canHelp ()} returns {@code true}.
     */
    void performHelp();

    /**
     * Sets the current context for this page.
     *
     * @param pageContext The context of the page.
     */
    void setContext(AssistantPageContext pageContext);

    /**
     * Gets the current context for this page.
     *
     * @return The context of the page.
     */
    AssistantPageContext getContext();
}
