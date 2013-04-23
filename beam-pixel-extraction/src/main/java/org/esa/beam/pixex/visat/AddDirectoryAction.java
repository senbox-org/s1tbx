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

package org.esa.beam.pixex.visat;

import com.bc.ceres.binding.ValidationException;
import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * @author Thomas Storm
 */
class AddDirectoryAction extends AbstractAction {

    private boolean recursive;
    private AppContext appContext;
    private InputListModel listModel;

    AddDirectoryAction(AppContext appContext, InputListModel listModel, boolean recursive) {
        this(recursive);
        this.appContext = appContext;
        this.listModel = listModel;
    }

    private AddDirectoryAction(boolean recursive) {
        this("Add directory" + (recursive ? " recursively" : "(s)") + "...");
        this.recursive = recursive;
    }

    private AddDirectoryAction(String title) {
        super(title);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final FolderChooser folderChooser = new FolderChooser();

        final PropertyMap preferences = appContext.getPreferences();
        String lastDir = preferences.getPropertyString(PixelExtractionIOForm.LAST_OPEN_INPUT_DIR,
                                                       SystemUtils.getUserHomeDir().getPath());
        if (lastDir != null) {
            folderChooser.setCurrentDirectory(new File(lastDir));
        }
        folderChooser.setMultiSelectionEnabled(!recursive);

        final Window parent = appContext.getApplicationWindow();

        final int result = folderChooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final String defaultPattern = recursive ? "*.dim" : "*";
        final FileSelectionPatternDialog dialog = new FileSelectionPatternDialog(defaultPattern, parent, PixelExtractionDialog.HELP_ID_JAVA_HELP);
        if (dialog.show() != ModalDialog.ID_OK) {
            return;
        }
        final String pattern = dialog.getPattern();

        File[] selectedDirs;
        if (recursive) {
            File selectedDir = folderChooser.getSelectedFolder();
            selectedDir = new File(selectedDir, "**");
            selectedDir = new File(selectedDir, pattern);
            selectedDirs = new File[]{selectedDir};
        } else {
            final File[] selectedPaths = folderChooser.getSelectedFiles();
            selectedDirs = new File[selectedPaths.length];
            for (int i = 0; i < selectedPaths.length; i++) {
                File selectedFile = selectedPaths[i];
                selectedDirs[i] = new File(selectedFile, pattern);
            }
        }
        try {
            listModel.addElements(selectedDirs);
            preferences.setPropertyString(PixelExtractionIOForm.LAST_OPEN_INPUT_DIR,
                                          selectedDirs[0].getAbsolutePath());

        } catch (ValidationException ve) {
            // not expected to ever come here
            appContext.handleError("Invalid input path", ve);
        }
    }

}
