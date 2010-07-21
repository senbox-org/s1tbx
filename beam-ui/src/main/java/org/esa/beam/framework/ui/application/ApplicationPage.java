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
