/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
        ApplicationPage page = VisatApp.getApp().getApplicationPage();
        ToolView toolView = page.getToolView(LayerEditorToolView.ID);
        if (toolView != null) {
            page.showToolView(LayerEditorToolView.ID);
        }
    }
}
