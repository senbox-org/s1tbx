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

package org.esa.beam.visat.actions;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;

import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;


/**
 * <p><b>WARNING:</b> This class belongs to a preliminary API and may change in future releases.<p/>
 * 
 * <p>An action which creates a default dialog for an operator given by the
 * action property action property {@code operatorName}.</p>
 * <p>Optionally the dialog title can be set via the {@code dialogTitle} property and
 * the ID of the help page can be given using the {@code helpId} property. If not given the
 * name of the operator will be used instead. Also optional the 
 * file name suffix for the target product can be given via the {@code targetProductNameSuffix} property.</p>
 *
 * @author Norman Fomferra
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 */
public class DefaultOperatorAction extends AbstractVisatAction {

    private ModelessDialog dialog;
    private String operatorName;
    private String dialogTitle;
    private String targetProductNameSuffix;

    @Override
    public void actionPerformed(CommandEvent event) {
      if (dialog == null) {
            dialog = createOperatorDialog();
        }
        dialog.show();
    }
    
    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        operatorName = getConfigString(config, "operatorName");
        if (operatorName == null) {
            throw new CoreException("Missing DefaultOperatorAction property 'operatorName'.");
        }
        dialogTitle = getValue(config, "dialogTitle", operatorName);
        targetProductNameSuffix = getConfigString(config, "targetProductNameSuffix");
        super.configure(config);
    }

    protected ModelessDialog createOperatorDialog() {
        DefaultSingleTargetProductDialog productDialog = new DefaultSingleTargetProductDialog(operatorName, getAppContext(),
                                                    dialogTitle, getHelpId());
        if (targetProductNameSuffix != null) {
            productDialog.setTargetProductNameSuffix(targetProductNameSuffix);
        }
        return productDialog;
    }
}
