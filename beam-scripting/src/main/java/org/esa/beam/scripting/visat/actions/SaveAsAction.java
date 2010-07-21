/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.scripting.visat.actions;

import org.esa.beam.scripting.visat.ScriptConsoleForm;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.MessageFormat;
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
        JFileChooser fileChooser = OpenAction.createFileChooser(engineFactory);
        while (true) {
            int i = fileChooser.showSaveDialog(getScriptConsoleForm().getContentPanel());
            if (i != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null) {
                return;
            }

            File file = fileChooser.getSelectedFile();
            if (extensions.size() > 0) {
                file = ensureFileNameWithExtension(file, extensions);
            }
            if (!file.exists()) {
                getScriptConsoleForm().saveScriptAs(file);
                return;
            } else {
                String msg = MessageFormat.format("File ''{0}'' already exists.\n" +
                        "Do you want to overwrite it?", file.getName());
                int ret = JOptionPane.showConfirmDialog(getScriptConsoleForm().getContentPanel(),
                                                        msg,
                                                        "Save Script",
                                                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (ret == JOptionPane.YES_OPTION) {
                    getScriptConsoleForm().saveScriptAs(file);
                    return;
                } else if (ret == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
        }
    }

    private File ensureFileNameWithExtension(File file, List<String> extensions) {
        String name = file.getName();
        int extPos = name.lastIndexOf('.');
        String ext = "";
        if (extPos > 0) {
            ext = name.substring(extPos + 1);
        }
        boolean extFound = false;
        for (String s : extensions) {
            if (ext.equals(s)) {
                extFound = true;
                break;
            }

        }
        if (!extFound) {
            String defaultExt = extensions.get(0);
            file = new File(file.getParent(), name + "." + defaultExt);
        }
        return file;
    }

}