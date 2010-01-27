package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import javax.script.ScriptEngineFactory;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.util.List;

public class OpenAction extends ScriptConsoleAction {

    public OpenAction(ScriptConsoleForm scriptConsoleForm ) {
        super(scriptConsoleForm,
              "Open...",
              "scriptConsole.open",
              "/org/esa/beam/scripting/visat/icons/document-open-16.png");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fs = OpenAction.createFileChooser(getScriptManager().getEngineFactories());
        int i = fs.showOpenDialog(getScriptConsoleForm().getWindow());
        if (i == JFileChooser.APPROVE_OPTION) {
            // todo 
            getScriptConsoleForm().showErrorMessage("Not implemented.");
        }
    }

    public static JFileChooser createFileChooser(ScriptEngineFactory[] scriptEngineFactories) {
        JFileChooser fs = new JFileChooser();
        fs.setAcceptAllFileFilterUsed(false);
        for (ScriptEngineFactory scriptEngineFactory : scriptEngineFactories) {
            final List<String> stringList = scriptEngineFactory.getExtensions();
            final String[] strings = stringList.toArray(new String[0]);
            fs.addChoosableFileFilter(new FileNameExtensionFilter(String.format("%s files", scriptEngineFactory.getLanguageName()), strings));
        }
        return fs;
    }
}
