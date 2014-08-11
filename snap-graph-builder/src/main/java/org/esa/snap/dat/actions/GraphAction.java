/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dat.actions;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.snap.dat.graphbuilder.GraphBuilderDialog;

import java.io.File;

/**
 * <p>An action which creates a default dialog for an operator given by the
 * action property action property {@code operatorName}.</p>
 * <p>Optionally the dialog title can be set via the {@code dialogTitle} property and
 * the ID of the help page can be given using the {@code helpId} property. If not given the
 * name of the operator will be used instead. Also optional the
 * file name suffix for the target product can be given via the {@code targetProductNameSuffix} property.</p>
 */
public class GraphAction extends OperatorAction {

    private String graphFileName;
    private boolean enableEditing = false;

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);

        graphFileName = getConfigString(config, "graphFile");
        String enableEditingStr = getConfigString(config, "enableEditing");
        if (enableEditingStr != null) {
            enableEditing = enableEditingStr.equalsIgnoreCase("true");
        }
    }

    @Override
    protected ModelessDialog createOperatorDialog() {
        final GraphBuilderDialog dialog = new GraphBuilderDialog(getAppContext(), dialogTitle, getHelpId(), enableEditing);
        dialog.show();

        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
        final File graphFile = new File(graphPath, graphFileName);

        addIcon(dialog);
        dialog.LoadGraph(graphFile);
        return dialog;
    }
}