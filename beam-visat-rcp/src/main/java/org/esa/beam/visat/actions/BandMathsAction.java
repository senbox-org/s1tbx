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
import org.esa.beam.framework.datamodel.ProductNodeList;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.BandMathsDialog;


/**
 * VISAT's band arithmetic feature.
 *
 * @author Norman Fomferra
 * @version $Revision: 8410 $ $Date: 2010-02-14 14:31:41 +0100 (So, 14 Feb 2010) $
 */
public class BandMathsAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        openBandArithmeticDialog(VisatApp.getApp(), event.getCommand().getHelpId());
    }

    @Override
    public void updateState(final CommandEvent event) {
        final int n = VisatApp.getApp().getProductManager().getProductCount();
        setEnabled(n > 0 && VisatApp.getApp().getSelectedProduct() != null); 
    }

   
    private void openBandArithmeticDialog(final VisatApp visatApp, final String helpId) {

        final Product[] prods = visatApp.getProductManager().getProducts();
        final ProductNodeList<Product> products = new ProductNodeList<Product>();
        for (Product prod : prods) {
            products.add(prod);
        }
        BandMathsDialog bandMathsDialog = new BandMathsDialog(visatApp,
                                                         visatApp.getSelectedProduct(),
                                                         products,
                                                         helpId);
        bandMathsDialog.show();
    }
}
