package org.esa.beam.framework.ui.application;

public interface ApplicationPage extends ControlFactory {

    ApplicationWindow getWindow();

    /**
     * Called by the framework.
     *
     * @param window the window
     */
    void setWindow(ApplicationWindow window);

    void addPageComponentListener(PageComponentListener listener);

    void removePageComponentListener(PageComponentListener listener);

    PageComponent getActiveComponent();

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
