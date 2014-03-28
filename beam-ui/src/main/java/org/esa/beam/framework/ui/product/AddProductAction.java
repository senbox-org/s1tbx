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

package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.Debug;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Thomas Storm
 */
class AddProductAction extends AbstractAction {

    private final AppContext appContext;
    private final InputListModel listModel;

    AddProductAction(AppContext appContext, InputListModel listModel) {
        super("Add product(s)...");
        this.appContext = appContext;
        this.listModel = listModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ProductChooser productChooser = new ProductChooser(appContext.getApplicationWindow(), "Add product",
                                                           ModalDialog.ID_OK_CANCEL, null,
                                                           filterProducts());
        if (productChooser.show() != ModalDialog.ID_OK) {
            return;
        }
        try {
            listModel.addElements(productChooser.getSelectedProducts().toArray());
        } catch (ValidationException ve) {
            Debug.trace(ve);
        }
    }

    private Product[] filterProducts() {
        List<Product> currentlyOpenedProducts = Arrays.asList(listModel.getSourceProducts());
        List<Product> productManagerProducts = Arrays.asList(appContext.getProductManager().getProducts());
        ArrayList<Product> result = new ArrayList<>();
        for (Product product : productManagerProducts) {
            if (!currentlyOpenedProducts.contains(product)) {
                result.add(product);
            }
        }
        return result.toArray(new Product[result.size()]);
    }

}
