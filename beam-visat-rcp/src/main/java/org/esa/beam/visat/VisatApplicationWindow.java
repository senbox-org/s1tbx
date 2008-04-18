package org.esa.beam.visat;

import org.esa.beam.framework.ui.application.ApplicationWindow;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.application.PageComponentService;
import org.esa.beam.framework.ui.application.SelectionService;
import org.esa.beam.framework.ui.application.support.DefaultSelectionService;
import org.esa.beam.framework.ui.command.CommandManager;

import javax.swing.*;
import java.util.Iterator;

// todo - Status-quo: VisatApp IS-A ApplicationWindow  and IS-A Application
// todo - Goal: VisatApp IS-A Application and HAS-A ApplicationWindow
class VisatApplicationWindow implements ApplicationWindow {
    private VisatApp app;
    private VisatApplicationPage page;
    private final SelectionService selectionService;

    public VisatApplicationWindow(VisatApp app) {
        this.app = app;
        selectionService = new DefaultSelectionService();
    }

    void setPage(VisatApplicationPage page) {
        this.page = page;
    }

    public ApplicationPage getPage() {
        return page;
    }

    public PageComponentService getPageComponentService() {
        return page;
    }

    public SelectionService getSelectionService() {
        return selectionService;
    }

    public CommandManager getCommandManager() {
        return app.getCommandManager();
    }

    public Iterator getSharedCommands() {
        // todo
        throw new IllegalStateException("not implemented");
    }

    public JMenuBar getMenuBar() {
        return app.getMainFrame().getJMenuBar();
    }

    public JPanel getToolBar() {
        // todo
        throw new IllegalStateException("not implemented");
    }

    public JPanel getStatusBar() {
        // todo
        throw new IllegalStateException("not implemented");
    }

    public JFrame getControl() {
        return app.getMainFrame();
    }

    public void show() {
        // todo
        throw new IllegalStateException("not implemented");
    }

    public boolean close() {
        // todo
        throw new IllegalStateException("not implemented");
    }
}
