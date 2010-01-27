package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import java.awt.event.ActionEvent;

public class SaveAction extends ScriptConsoleAction {

    public SaveAction(ScriptConsoleForm scriptConsoleForm) {
        super(scriptConsoleForm,
              "Save",
              "scriptConsole.save",
              "/org/esa/beam/scripting/visat/icons/document-save-16.png");
    }

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // todo
        getScriptConsoleForm().showErrorMessage("Not implemented.");
    }
}