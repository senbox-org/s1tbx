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
package org.esa.s1tbx.utilities.rcp.actions;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;

import java.awt.event.ActionEvent;

/*
@ActionID(category = "Raster", id = "org.esa.s1tbx.dat.ScaleDataAction" )
@ActionRegistration(displayName = "#CTL_ScaleDataAction_Text")
@ActionReference(path = "Menu/Raster/Data Conversion", position = 300 )
@NbBundle.Messages({"CTL_ScaleDataAction_Text=Scale Data"})
*/

/**
 * ScaleData action.
 */
public class ScaleDataAction extends AbstractSnapAction {

    @Override
    public void actionPerformed(ActionEvent event) {

        final ProductNode node = SnapApp.getDefault().getSelectedProductNode();
        if (node instanceof Band) {
            final Band band = (Band) node;
            final Product product = band.getProduct();

            ScaleDataDialog dlg = new ScaleDataDialog("Scaling Data", product, band);
            dlg.show();
        }
    }

// Code removed by nf, lv to review
//    public void updateState(CommandEvent event) {
//        final ProductNode node = SnapApp.getDefault().getSelectedProductNode();
//        if (node instanceof Band) {
//            final Band band = (Band) node;
//            final String unit = band.getUnit();
//            if (unit != null && !unit.contains(Unit.PHASE)) {
//                event.getCommand().setEnabled(true);
//                return;
//            }
//        }
//        event.getCommand().setEnabled(false);
//    }

}
