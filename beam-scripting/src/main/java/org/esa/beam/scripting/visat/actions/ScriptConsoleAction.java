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