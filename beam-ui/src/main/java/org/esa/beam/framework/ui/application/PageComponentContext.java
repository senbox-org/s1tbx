package org.esa.beam.framework.ui.application;

/**
 * Mediator between the application and the view. The application uses this
 * class to get the view's local action handlers. The view uses this class to
 * get information about how the view is displayed in the application (for
 * example, on which window).
 * <p/>
 * <p>Clients shall not implement this interface, it is provided by the framework via the
 * {@link PageComponent#getContext} method.</p>
 *
 * @author Norman Fomferra (original by Keith Donald of Spring RCP project)
 */
public interface PageComponentContext {
    /**
     * Gets the application's page.
     *
     * @return The application page.
     */
    ApplicationPage getPage();

    /**
     * Gets the associated page component pane.
     *
     * @return The page component pane.
     */
    PageComponentPane getPane();
}
