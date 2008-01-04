package org.esa.beam.framework.ui.application;

import org.esa.beam.framework.ui.command.CommandManager;

import javax.swing.*;
import java.util.Iterator;

public interface ApplicationWindow {
    ApplicationPage getPage();

    CommandManager getCommandManager();

    SelectionService getSelectionService();

    PageComponentService getPageComponentService();

    Iterator getSharedCommands();

    JMenuBar getMenuBar();

    JPanel getToolBar();

    JPanel getStatusBar();

    JFrame getControl();

    void show();

    boolean close();

}
