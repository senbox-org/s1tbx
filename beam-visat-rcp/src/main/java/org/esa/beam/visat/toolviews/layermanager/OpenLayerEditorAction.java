package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

class OpenLayerEditorAction extends AbstractAction {

    OpenLayerEditorAction() {
        super("Open Layer Editor", UIUtils.loadImageIcon("icons/LayerEditor24.png"));
        putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ApplicationPage page = VisatApp.getApp().getPage();
        ToolView toolView = page.getToolView(LayerEditorToolView.ID);
        if (toolView != null) {
            page.showToolView(LayerEditorToolView.ID);
        }
    }
}
