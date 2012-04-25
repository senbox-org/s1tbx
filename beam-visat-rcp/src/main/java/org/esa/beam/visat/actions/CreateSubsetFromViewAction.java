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

package org.esa.beam.visat.actions;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductSubsetDialog;
import org.esa.beam.visat.VisatApp;

import java.awt.*;

public class CreateSubsetFromViewAction extends ExecCommand {

    private int subsetNumber;
    private static final String DIALOG_TITLE = "Create Subset from View";

    @Override
    public void actionPerformed(CommandEvent event) {
        VisatApp visatApp = VisatApp.getApp();
        final ProductSceneView psv = visatApp.getSelectedProductSceneView();
        if (psv != null) {
            final Rectangle bounds = psv.getVisibleImageBounds();
            final String propertyName = null;
            if (bounds == null) {
                visatApp.showInfoDialog(DIALOG_TITLE,
                                        "The viewing area is completely outside of the product bounds.", /*I18N*/
                                        propertyName);
                return;
            }

            final Product sourceProduct = psv.getProduct();
            final String subsetName = "subset_" + subsetNumber + "_of_" + sourceProduct.getName();
            final ProductSubsetDef initSubset = new ProductSubsetDef();
            initSubset.setRegion(bounds);
            initSubset.setNodeNames(sourceProduct.getBandNames());
            initSubset.addNodeNames(sourceProduct.getTiePointGridNames());
            initSubset.setIgnoreMetadata(false);
            final ProductSubsetDialog subsetDialog = new ProductSubsetDialog(visatApp.getMainFrame(),
                                                                             sourceProduct, initSubset);
            if (subsetDialog.show() != ProductSubsetDialog.ID_OK) {
                return;
            }
            final ProductSubsetDef subsetDef = subsetDialog.getProductSubsetDef();
            if (subsetDef == null) {
                visatApp.showInfoDialog(DIALOG_TITLE,
                                        "The selected subset is equal to the entire product.\n" + /*I18N*/
                                                "So no subset was created.",
                                        propertyName);
                return;
            }
            try {
                final Product subset = sourceProduct.createSubset(subsetDef, subsetName,
                                                                  sourceProduct.getDescription());
                visatApp.getProductManager().addProduct(subset);
                subsetNumber++;
            } catch (Exception e) {
                final String msg = "Unable to create the product subset because\n" +
                        "an error occurred at creating the subset.\n" +
                        e.getMessage();
                visatApp.handleError(msg, e);
            }
        }
    }

    @Override
    public void updateState(CommandEvent event) {
        event.getCommand().setEnabled(VisatApp.getApp().getSelectedProductSceneView() != null);
    }

}
