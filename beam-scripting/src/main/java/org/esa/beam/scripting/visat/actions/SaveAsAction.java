package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.swing.JFileChooser;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class SaveAsAction extends ScriptConsoleAction {
    public static final String ID = "scriptConsole.saveAs";

    public SaveAsAction(ScriptConsoleForm scriptConsoleForm) {
        super(scriptConsoleForm,
              "Save as...",
              ID,
              "/org/esa/beam/scripting/visat/icons/document-save-as-16.png");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ScriptEngine scriptEngine = getScriptManager().getEngine();
        ScriptEngineFactory engineFactory = scriptEngine.getFactory();
        List<String> extensions = engineFactory.getExtensions();
        JFileChooser fs = OpenAction.createFileChooser(engineFactory);
        int i = fs.showSaveDialog(getScriptConsoleForm().getWindow());
        if (i == JFileChooser.APPROVE_OPTION) {
            File file = fs.getSelectedFile();
            if (extensions.size()>0) {
                String name = file.getName();
                int extPos = name.lastIndexOf('.');
                String ext = "";
                if (extPos <=0) {
                    ext=name.substring(extPos+1);
                }
                if (!ext.equals(extensions.get(0))) {
                    file = new File(file.getParent(), name + "."+extensions.get(0));
                }
            }
            if (file.exists()) {
                // todo
            }
            getScriptConsoleForm().saveScriptAs(file);
        }
    }

}