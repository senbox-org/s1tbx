package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import java.awt.event.ActionEvent;

public class StopAction extends ScriptConsoleAction {
    public static final String ID = "scriptConsole.stop";

    public StopAction(ScriptConsoleForm scriptConsoleForm) {
        super(scriptConsoleForm,
              "Stop",
              ID,
              "/org/esa/beam/scripting/visat/icons/process-stop-16.png");
    }

    public void actionPerformed(ActionEvent e) {
        getScriptConsoleForm().stopScript();
    }
}
