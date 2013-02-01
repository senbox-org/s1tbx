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
package org.esa.nest.dat.actions;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.visat.actions.DefaultOperatorAction;
import org.esa.nest.dat.dialogs.NestSingleTargetProductDialog;
import org.esa.nest.util.ResourceUtils;

import javax.swing.*;

/**
 * <p>An action which creates a default dialog for an operator given by the
 * action property action property {@code operatorName}.</p>
 * <p>Optionally the dialog title can be set via the {@code dialogTitle} property and
 * the ID of the help page can be given using the {@code helpId} property. If not given the
 * name of the operator will be used instead. Also optional the
 * file name suffix for the target product can be given via the {@code targetProductNameSuffix} property.</p>
 */
public class OperatorAction extends DefaultOperatorAction {
    private String iconName;

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);
        iconName = getConfigString(config, "icon");
    }

    @Override
    protected ModelessDialog createOperatorDialog() {
        final NestSingleTargetProductDialog dialog = new NestSingleTargetProductDialog(operatorName,
                                                    getAppContext(),  dialogTitle, getHelpId());
        if (targetProductNameSuffix != null) {
            dialog.setTargetProductNameSuffix(targetProductNameSuffix);
        }
        addIcon(dialog);
        return dialog;
    }

    protected void addIcon(final ModelessDialog dlg) {
        if(iconName == null) {
            setIcon(dlg, ResourceUtils.nestIcon);
        } else if(iconName.equals("esaIcon")) {
            setIcon(dlg, ResourceUtils.esaPlanetIcon);
        } else if(iconName.equals("rstbIcon")) {
            setIcon(dlg, ResourceUtils.rstbIcon);
        } else if(iconName.equals("geoAusIcon")) {
            setIcon(dlg, ResourceUtils.geoAusIcon);
        } else {
            final ImageIcon icon = ResourceUtils.LoadIcon(iconName);
            if(icon != null)
                setIcon(dlg, icon);
        }
    }

    private static void setIcon(final ModelessDialog dlg, final ImageIcon ico) {
        if(ico == null) return;
        dlg.getJDialog().setIconImage(ico.getImage());
    }
}
