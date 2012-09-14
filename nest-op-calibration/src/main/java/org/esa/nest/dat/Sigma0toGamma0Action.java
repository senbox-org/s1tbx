/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.CalibrationOp;

/**
 * Sigma0toGamma0Action action.
 *
 */
public class Sigma0toGamma0Action extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {

        CalibrationOp.createGammaVirtualBand(VisatApp.getApp().getSelectedProduct(), false);
    }

    @Override
    public void updateState(CommandEvent event) {
        final ProductNode node = VisatApp.getApp().getSelectedProductNode();
        if(node instanceof Band) {
            final Band band = (Band) node;
            final String unit = band.getUnit();
            if(unit != null && unit.contains(Unit.INTENSITY) && band.getName().toLowerCase().contains("sigma")) {
                event.getCommand().setEnabled(true);
                return;
            }
        }
        event.getCommand().setEnabled(false);
    }

}