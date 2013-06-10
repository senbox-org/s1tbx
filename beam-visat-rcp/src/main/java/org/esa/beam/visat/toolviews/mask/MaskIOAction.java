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

package org.esa.beam.visat.toolviews.mask;

import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.JOptionPane;
import java.io.File;

abstract class MaskIOAction extends MaskAction {

    private final AbstractToolView maskToolView;

    public MaskIOAction(AbstractToolView maskToolView, MaskForm maskForm, String iconPath, String buttonName,
                        String description
    ) {
        super(maskForm, iconPath, buttonName, description);
        this.maskToolView = maskToolView;
    }

    public AbstractToolView getMaskToolView() {
        return maskToolView;
    }

    void showErrorDialog(final String message) {
        JOptionPane.showMessageDialog(maskToolView.getPaneWindow(),
                                      message,
                                      maskToolView.getDescriptor().getTitle() + " - Error",    /*I18N*/
                                      JOptionPane.ERROR_MESSAGE);
    }

    void setDirectory(final File directory) {
        if (VisatApp.getApp().getPreferences() != null && directory != null) {
            VisatApp.getApp().getPreferences().setPropertyString("mask.io.dir", directory.getPath());
        }
    }

    File getDirectory() {
        File directory = SystemUtils.getUserHomeDir();
        if (VisatApp.getApp().getPreferences() != null) {
            directory = new File(VisatApp.getApp().getPreferences().getPropertyString("mask.io.dir",
                                                                                      directory.getPath()));
        }
        return directory;
    }
}
