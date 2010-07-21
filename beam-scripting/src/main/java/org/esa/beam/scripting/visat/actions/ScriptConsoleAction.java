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

package org.esa.beam.scripting.visat.actions;

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.scripting.visat.ScriptConsoleForm;
import org.esa.beam.scripting.visat.ScriptManager;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

public abstract class ScriptConsoleAction extends AbstractAction {
    private final ScriptConsoleForm scriptConsoleForm;

    protected ScriptConsoleAction(ScriptConsoleForm scriptConsoleForm, String name, String commandKey, String iconResource) {
        this.scriptConsoleForm = scriptConsoleForm;
        putValue(AbstractAction.NAME, name);
        putValue(AbstractAction.ACTION_COMMAND_KEY, commandKey);
        final ImageIcon icon = loadIcon(iconResource);
        putValue(AbstractAction.SMALL_ICON, icon);
        putValue(AbstractAction.LARGE_ICON_KEY, icon);
    }

    protected ImageIcon loadIcon(String iconResource) {
        return UIUtils.loadImageIcon(iconResource, getClass());
    }

    public ScriptConsoleForm getScriptConsoleForm() {
        return scriptConsoleForm;
    }

    public ScriptManager getScriptManager() {
        return scriptConsoleForm.getScriptManager();
    }
}