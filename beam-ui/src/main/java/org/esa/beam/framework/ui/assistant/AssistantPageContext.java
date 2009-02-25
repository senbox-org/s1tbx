package org.esa.beam.framework.ui.assistant;

import java.awt.Window;

public interface AssistantPageContext {

    Window getWindow();

    AssistantPage getCurrentPage();

    void setCurrentPage(AssistantPage page);

    void updateState();

    void showErrorDialog(String message);
}
