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
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.jidesoft.swing.FolderChooser;
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
class AddDirectoryAction extends AbstractAction {

    private boolean recursive;
    private AppContext appContext;
    private InputFilesListModel listModel;

    AddDirectoryAction(AppContext appContext, InputFilesListModel listModel, boolean recursive) {
        this(recursive);
        this.appContext = appContext;
        this.listModel = listModel;
    }

    private AddDirectoryAction(boolean recursive) {
        this("Add directory" + (recursive ? " recursively" : ""));
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

        final int result = folderChooser.showOpenDialog(appContext.getApplicationWindow());
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File currentDir = folderChooser.getSelectedFolder();

        if (!recursive) {
            try {
                listModel.addElement(currentDir);
            } catch (ValidationException ve) {
                // not expected to ever come here
                appContext.handleError("Invalid input path", ve);
            }
        } else {
            new MyProgressMonitorSwingWorker(currentDir).executeWithBlocking();
        }

        preferences.setPropertyString(PixelExtractionIOForm.LAST_OPEN_INPUT_DIR,
                                      currentDir.getAbsolutePath());

    }

    private void addFiles(File[] files) throws ValidationException {
        for (File file : files) {
            if (file.isDirectory()) {
                listModel.addElement(file);
                addFiles(file.listFiles());
            }
        }
    }

    private class MyProgressMonitorSwingWorker extends ProgressMonitorSwingWorker<Void, Void> {

        private final File currentDir;

        public MyProgressMonitorSwingWorker(File currentDir) {
            super(appContext.getApplicationWindow(), "Collecting source directories");
            this.currentDir = currentDir;
        }

        @Override
        protected Void doInBackground(ProgressMonitor pm) throws Exception {
            pm.beginTask("Collecting source directories", 1);
            listModel.addElement(currentDir);
            addFiles(currentDir.listFiles());
            pm.done();
            return null;
        }
    }
}
