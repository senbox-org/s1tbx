/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.s1tbx.dat.actions;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.file.CloseProductAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * This action closes all opened products other than the one selected.
 */
@ActionID(
        category = "File",
        id = "CloseAllOthersAction"
)
@ActionRegistration(
        displayName = "#CTL_CloseAllOthersActionName"
)
@ActionReference(path = "Menu/File", position = 50)
@NbBundle.Messages({
        "CTL_CloseAllOthersActionName=Close All Other Products"
})
public class CloseAllOthersAction extends AbstractAction {      //todo make context aware

    @Override
    public void actionPerformed(final ActionEvent event) {
        final Product selectedProduct = SnapApp.getDefault().getSelectedProduct();
        final Product[] products = SnapApp.getDefault().getProductManager().getProducts();
        final List<Product> productsToClose = new ArrayList<>(products.length);
        for (Product product : products) {
            if (product != selectedProduct) {
                productsToClose.add(product);
            }
        }
        new CloseProductAction(productsToClose).execute();
    }
}
