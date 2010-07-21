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

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.ProductFlipDialog;

public class FlippingAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        openFlippingDialog(VisatApp.getApp(), getHelpId());
    }

    @Override
    public void updateState(final CommandEvent event) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        setEnabled(product != null && product.getNumBands() + product.getNumTiePointGrids() > 0);
    }

    private static void openFlippingDialog(final VisatApp visatApp, final String helpId) {

        final Product partentProduct = visatApp.getSelectedProduct();
        final ProductFlipDialog dialog = new ProductFlipDialog(visatApp.getMainFrame(), partentProduct);
        if (dialog.show() == ModalDialog.ID_OK) {
            final Product product = dialog.getResultProduct();
            if (product != null) {
                visatApp.addProduct(product);
            } else if (dialog.getException() != null) {
                visatApp.showErrorDialog("The flipped product could not be created:\n" +
                                         dialog.getException().getMessage());
            }
        }
    }

}
