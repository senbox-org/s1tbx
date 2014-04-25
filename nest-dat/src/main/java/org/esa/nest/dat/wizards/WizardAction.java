/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.wizards;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.actions.OperatorAction;
import org.esa.nest.util.ResourceUtils;

/**
 * <p>An action which creates a default dialog for an operator given by the
 * action property action property {@code operatorName}.</p>
 * <p>Optionally the dialog title can be set via the {@code dialogTitle} property and
 * the ID of the help page can be given using the {@code helpId} property. If not given the
 * name of the operator will be used instead. Also optional the
 * file name suffix for the target product can be given via the {@code targetProductNameSuffix} property.</p>
 */
public class WizardAction extends OperatorAction {
    WizardPanel wizardPanel;

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);

        try {
            String panelClass = getConfigString(config, "wizardPanelClass");
            Class wizardClass = config.getDeclaringExtension().getDeclaringModule().loadClass(panelClass);

            wizardPanel = (WizardPanel)wizardClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected ModelessDialog createOperatorDialog() {

        final WizardDialog dialog = new WizardDialog(VisatApp.getApp().getMainFrame(), false,
                dialogTitle, getHelpId(), wizardPanel);
        dialog.setIcon(ResourceUtils.rstbIcon);
        dialog.setVisible(true);
        return null;
    }
}