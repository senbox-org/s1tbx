package org.esa.beam.framework.ui.assistant;

import java.awt.Window;

/**
 * Instances of this interface provide the context for
 * implementations of {@link AssistantPage}.
 */
public interface AssistantPageContext {

    /**
     * The window in which the {@link AssistantPage} is shown.
     *
     * @return The window.
     */
    Window getWindow();

    /**
     * Gets the currently displayed {@link AssistantPage page}.
     *
     * @return The current page.
     */
    AssistantPage getCurrentPage();

    /**
     * Sets the currently displayed {@link AssistantPage page}.
     * Should only be called by the framwoerk.
     *
     * @param page The current page.
     */
    void setCurrentPage(AssistantPage page);

    /**
     * Forces an updated of the state.
     */
    void updateState();

    /**
     * Shows an error dialog with the given message.
     *
     * @param message The error message to display.
     */
    void showErrorDialog(String message);
}
