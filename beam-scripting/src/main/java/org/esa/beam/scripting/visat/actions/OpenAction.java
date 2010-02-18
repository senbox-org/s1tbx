package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import javax.script.ScriptEngineFactory;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.util.List;

public class OpenAction extends ScriptConsoleAction {
    public static final String ID = "scriptConsole.open";

    public OpenAction(ScriptConsoleForm scriptConsoleForm) {
        super(scriptConsoleForm,
              "Open...",
              ID,
              "/org/esa/beam/scripting/visat/icons/document-open-16.png");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fs = OpenAction.createFileChooser(getScriptManager().getEngineFactories());
        int i = fs.showOpenDialog(getScriptConsoleForm().getContentPanel());
        if (i == JFileChooser.APPROVE_OPTION) {
            getScriptConsoleForm().openScript(fs.getSelectedFile());
        }
    }

    public static JFileChooser createFileChooser(ScriptEngineFactory[] scriptEngineFactories) {
        JFileChooser fs = new JFileChooser();
        fs.setAcceptAllFileFilterUsed(false);
        for (ScriptEngineFactory scriptEngineFactory : scriptEngineFactories) {
            FileNameExtensionFilter filter = createFileFilter(scriptEngineFactory);
            fs.addChoosableFileFilter(filter);
        }
        return fs;
    }

    public static JFileChooser createFileChooser(ScriptEngineFactory scriptEngineFactory) {
        JFileChooser fs = new JFileChooser();
        fs.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = createFileFilter(scriptEngineFactory);
        fs.addChoosableFileFilter(filter);
        return fs;
    }

    public static FileNameExtensionFilter createFileFilter(ScriptEngineFactory scriptEngineFactory) {
        List<String> extensionList = scriptEngineFactory.getExtensions();
        String[] extensions = extensionList.toArray(new String[extensionList.size()]);
        String description = String.format("%s files", scriptEngineFactory.getLanguageName());
        return new FileNameExtensionFilter(description, extensions);
    }
}
