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

package org.esa.beam.pet.visat;

import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @author Thomas Storm
 */
class AddProductAction extends AbstractAction {

    private final AppContext appContext;
    private final InputFilesListModel listModel;

    AddProductAction(AppContext appContext, InputFilesListModel listModel) {
        super("Add product(s)");
        this.appContext = appContext;
        this.listModel = listModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ProductChooser productChooser = new ProductChooser(appContext.getApplicationWindow(), "Add product",
                                                           ModalDialog.ID_OK_CANCEL, "noHelpAvailable",
                                                           appContext.getProductManager().getProducts());
        if (productChooser.show() != ModalDialog.ID_OK) {
            return;
        }
        try {
            List<Product> selectedProducts = productChooser.getSelectedProducts();
            for (Product selectedProduct : selectedProducts) {
                listModel.addElement(selectedProduct.getFileLocation());
            }
        } catch (ValidationException ignore) {
            // do nothing
        }
    }
}
