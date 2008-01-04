package org.esa.beam.framework.ui.application;

public interface ApplicationPage extends ControlFactory, SelectionService, PageComponentService {

    ApplicationWindow getWindow();

    /**
     * Called by the framework.
     *
     * @param window the window
     */
    void setWindow(ApplicationWindow window);

    PageComponent getPageComponent(String id);

    ToolView[] getToolViews();

    ToolView getToolView(String id);

    ToolView addToolView(ToolViewDescriptor viewDescriptor);

    ToolView showToolView(String id);

    ToolView showToolView(ToolViewDescriptor viewDescriptor);

    void hideToolView(ToolView toolView);

    DocView openDocView(Object editorInput);

    void close(PageComponent pageComponent);

    boolean closeAllDocViews();

    boolean close();
}
