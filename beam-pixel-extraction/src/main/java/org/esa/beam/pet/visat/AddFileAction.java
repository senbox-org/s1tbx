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

package org.esa.beam.pet.visat;

import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * @author Thomas Storm
 */
class AddFileAction extends AbstractAction {

    private AppContext appContext;
    private InputFilesListModel listModel;

    AddFileAction(AppContext appContext, InputFilesListModel listModel) {
        super("Add single input product(s)");
        this.appContext = appContext;
        this.listModel = listModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final PropertyMap preferences = appContext.getPreferences();
        String lastDir = preferences.getPropertyString(PixelExtractionIOForm.BEAM_PET_OP_FILE_LAST_OPEN_DIR,
                                                       SystemUtils.getUserHomeDir().getPath());
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(lastDir));
        fileChooser.setDialogTitle("Select product(s)");
        fileChooser.setMultiSelectionEnabled(true);

        int result = fileChooser.showDialog(appContext.getApplicationWindow(), "Select product(s)");    /*I18N*/
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        preferences.setPropertyString(PixelExtractionIOForm.BEAM_PET_OP_FILE_LAST_OPEN_DIR,
                                      fileChooser.getCurrentDirectory().getAbsolutePath());

        final File[] selectedFiles = fileChooser.getSelectedFiles();
        try {
            listModel.addElement((Object[])selectedFiles);
        } catch (ValidationException ve) {
            // not expected to ever come here
            appContext.handleError("Invalid input path", ve);
        }
    }
}
