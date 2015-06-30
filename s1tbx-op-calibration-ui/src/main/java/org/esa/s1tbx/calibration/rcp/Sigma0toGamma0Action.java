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
package org.esa.s1tbx.calibration.rcp;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * Sigma0toGamma0Action action.
 */
@ActionID(category = "Tools", id = "Sigma0toGamma0Action" )
@ActionRegistration(displayName = "#CTL_Sigma0toGamma0ActionName" )
@ActionReference(path = "Menu/Radar/Radiometric", position = 700)
@NbBundle.Messages({ "CTL_Sigma0toGamma0ActionName=Convert Sigma0 to Gamma0" })
public class Sigma0toGamma0Action extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent event) {

        CalibrationOp.createGammaVirtualBand(SnapApp.getDefault().getSelectedProduct(), false);
    }

    public void updateState(CommandEvent event) {
        final ProductNode node = SnapApp.getDefault().getSelectedProductNode();
        if (node instanceof Band) {
            final Band band = (Band) node;
            final String unit = band.getUnit();
            if (unit != null && unit.contains(Unit.INTENSITY) && band.getName().toLowerCase().contains("sigma")) {
                event.getCommand().setEnabled(true);
                return;
            }
        }
        event.getCommand().setEnabled(false);
    }
}
