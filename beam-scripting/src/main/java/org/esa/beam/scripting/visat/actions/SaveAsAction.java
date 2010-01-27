package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import javax.swing.JFileChooser;
import java.awt.event.ActionEvent;

public class SaveAsAction extends ScriptConsoleAction {

    public SaveAsAction(ScriptConsoleForm scriptConsoleForm) {
        super(scriptConsoleForm,
              "Save as...",
              "scriptConsole.saveAs",
              "/org/esa/beam/scripting/visat/icons/document-save-as-16.png");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fs = OpenAction.createFileChooser(getScriptManager().getAvailableScriptEngineFactories());
        int i = fs.showSaveDialog(getScriptConsoleForm().getWindow());
        if (i == JFileChooser.APPROVE_OPTION) {
            // todo
            getScriptConsoleForm().showErrorMessage("Not implemented.");
        }
    }

}