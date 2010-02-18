package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import java.awt.event.ActionEvent;

public class RunAction extends ScriptConsoleAction {
    public static final String ID = "scriptConsole.run";

    public RunAction(ScriptConsoleForm scriptConsoleForm) {
        super(scriptConsoleForm,
              "Run",
              ID,
              "/org/esa/beam/scripting/visat/icons/media-playback-start-16.png");
    }

    public void actionPerformed(ActionEvent actionEvent) {
        getScriptConsoleForm().runScript();
    }
}
