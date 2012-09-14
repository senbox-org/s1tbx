/*
 * $Id: CloseAllOthersAction.java,v 1.3 2012-01-03 18:49:13 lveci Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.nest.dat.actions;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.MemUtils;

/**
 * This action closes all opened products other than the one selected.
 *
 */
public class CloseAllOthersAction extends ExecCommand {

    @Override
    public void actionPerformed(final CommandEvent event) {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final Product[] products = VisatApp.getApp().getProductManager().getProducts();
        for (int i = products.length - 1; i >= 0; i--) {
            if(products[i] != selectedProduct)
                VisatApp.getApp().closeProduct(products[i]);
        }
        // free cache
        MemUtils.freeAllMemory();
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getProductManager().getProductCount() > 1 &&
                VisatApp.getApp().getSelectedProduct() != null);
    }
}