package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import java.awt.event.ActionEvent;

public class StopAction extends ScriptConsoleAction {

    public StopAction(ScriptConsoleForm scriptConsoleForm) {
        super(scriptConsoleForm,
              "Stop",
              "scriptConsole.stop",
              "/org/esa/beam/scripting/visat/icons/process-stop-16.png");
    }

    public void actionPerformed(ActionEvent e) {
        getScriptConsoleForm().stopScript();
    }
}
