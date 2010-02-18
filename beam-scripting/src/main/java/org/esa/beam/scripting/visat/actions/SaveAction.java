package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import java.awt.event.ActionEvent;

public class SaveAction extends ScriptConsoleAction {
    public static final String ID = "scriptConsole.save";

    public SaveAction(ScriptConsoleForm scriptConsoleForm) {
        super(scriptConsoleForm,
              "Save",
              ID,
              "/org/esa/beam/scripting/visat/icons/document-save-16.png");
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (getScriptConsoleForm().getFile() != null) {
            getScriptConsoleForm().saveScript();
        } else {
            getScriptConsoleForm().getAction(SaveAsAction.ID).actionPerformed(e);
        }
    }
}