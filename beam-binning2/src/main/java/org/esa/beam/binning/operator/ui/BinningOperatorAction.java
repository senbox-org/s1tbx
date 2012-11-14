/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator.ui;

import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

/**
 * Action for starting the GPF binning operator user interface.
 *
 * @author Tonio Fincke
 * @author Thomas Storm
 */
public class BinningOperatorAction extends AbstractVisatAction {

    private ModelessDialog dialog;

    @Override
    public void actionPerformed(CommandEvent event) {
        if (dialog == null) {
            dialog = new BinningDialog(getAppContext(), "Binning", event.getCommand().getHelpId());
        }
        dialog.show();
    }

    @Override
    public boolean isEnabled() {
        return GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Binning") != null;
    }
}
