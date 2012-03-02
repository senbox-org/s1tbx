/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.aatsr.sst.visat;

import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.aatsr.sst.AatsrSstOp;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

public class AatsrSstAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final String operatorName = AatsrSstOp.Spi.class.getName();
        final AppContext appContext = getAppContext();
        final String title = "(A)ATSR SST Processor";
        final String helpID = event.getCommand().getHelpId();

        final DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog(operatorName, appContext,
                                                                                             title, helpID);
        final BindingContext bindingContext = dialog.getBindingContext();
        bindingContext.bindEnabledState("dual", true, "nadir", true);
        bindingContext.bindEnabledState("dualCoefficientsFile", true, "dual", true);
        bindingContext.bindEnabledState("dualMaskExpression", true, "dual", true);
        bindingContext.bindEnabledState("nadir", true, "dual", true);
        bindingContext.bindEnabledState("nadirCoefficientsFile", true, "nadir", true);
        bindingContext.bindEnabledState("nadirMaskExpression", true, "nadir", true);

        dialog.setTargetProductNameSuffix("_sst");
        dialog.getJDialog().pack();
        dialog.show();
    }
}
