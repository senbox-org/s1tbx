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
package org.esa.snap.core.param.editors;

import org.esa.snap.core.param.ParamProperties;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.SystemUtils;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * An editor for parameters of type {@link File}. This editor is composed of a text
 * field and a button labeled "...", which opens a file chooser.
 */
public class FileEditor extends TextFieldXEditor {

    private static final String _FILE_APPROVE_BUTTON_TEXT = "Select File"; /*I18N*/
    private static final String _DIR_APPROVE_BUTTON_TEXT = "Select Directory";   /*I18N*/
    private static final String _DEFAULT_APPROVE_BUTTON_TEXT = "Select";          /*I18N*/

    public FileEditor(Parameter parameter) {
        super(parameter);
        updateLastDir();
    }

    @Override
    protected void invokeXEditor() {
        File lastDir = getLastDir();
        File file = getParameterFileValue();
        JFileChooser fileChooser;
        int fsm = getParameter().getProperties().getFileSelectionMode();
        if (fsm == ParamProperties.FSM_DIRECTORIES_ONLY) {
            if (file == null) {
                file = lastDir;
            }
            fileChooser = createDirChooser(file);
        } else {
            fileChooser = createFileChooser(lastDir);
        }
        fileChooser.setFileSelectionMode(fsm);
        if (file != null && file.exists()) {
            fileChooser.setSelectedFile(file);
        }

        fileChooser.setDialogTitle(getXEditorTitle());
        final FileFilter[] choosable = getParameter().getProperties().getChoosableFileFilters();
        if (choosable != null) {
            for (int i = choosable.length - 1; i > -1; i--) {
                FileFilter filter = choosable[i];
                fileChooser.addChoosableFileFilter(filter);
            }
        }
        final FileFilter current = getParameter().getProperties().getCurrentFileFilter();
        if (current != null) {
            fileChooser.setFileFilter(current);
        }
        int option;
        if (fsm == ParamProperties.FSM_FILES_ONLY) {
            option = fileChooser.showDialog(getEditorComponent(), _FILE_APPROVE_BUTTON_TEXT);
        } else if (fsm == ParamProperties.FSM_DIRECTORIES_ONLY) {
            option = fileChooser.showDialog(getEditorComponent(), _DIR_APPROVE_BUTTON_TEXT);
        } else {
            option = fileChooser.showDialog(getEditorComponent(), _DEFAULT_APPROVE_BUTTON_TEXT);
        }
        setLastDir(fileChooser.getCurrentDirectory());
        if (option == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
            if (file != null) {
                setParameterFileValue(file);
            }
        }
    }

    private File getParameterFileValue() {
        return (File) getParameter().getValue();
    }

    private void setParameterFileValue(File file) {
        getParameter().setValue(file, null);
        updateLastDir();
    }

    private void updateLastDir() {
        File file = (File) getParameter().getValue();
        if (file != null) {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                setLastDir(parentFile);
            }
        }
    }

    private File getLastDir() {
        if (getParameter().getValue() instanceof File) {
            File file = (File) getParameter().getValue();
            final File parentDir = file.getParentFile();
            if (parentDir != null && parentDir.isDirectory()) {
                return parentDir;
            }
        }
        return (File) getParameter().getProperties().getPropertyValue(ParamProperties.LAST_DIR_KEY,
                                                                      SystemUtils.getApplicationHomeDir());
    }

    private void setLastDir(final File lastDir) {
        getParameter().getProperties().setPropertyValue(ParamProperties.LAST_DIR_KEY, lastDir);
    }

    public JFileChooser createFileChooser(File currentDirectory) {
        JFileChooser fileChooser = new JFileChooser(currentDirectory);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        return fileChooser;
    }

    public JFileChooser createDirChooser(File currentDirectory) {
        JFileChooser dirChooser = new JFileChooser(currentDirectory);
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return dirChooser;
    }

}
