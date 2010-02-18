package org.esa.beam.framework.ui.application;

import com.bc.ceres.swing.selection.SelectionManager;
import org.esa.beam.framework.ui.command.CommandManager;

import java.awt.Window;

public interface ApplicationPage extends ControlFactory, PageComponentService {

    Window getWindow();

    CommandManager getCommandManager();

    SelectionManager getSelectionManager();

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
