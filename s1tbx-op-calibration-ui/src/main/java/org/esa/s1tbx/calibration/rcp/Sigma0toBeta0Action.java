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
import org.esa.snap.visat.actions.AbstractVisatAction;

/**
 * Sigma0toBeta0Action action.
 */
public class Sigma0toBeta0Action extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {

        CalibrationOp.createBetaVirtualBand(SnapApp.getDefault().getSelectedProduct(), false);
    }

    @Override
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
