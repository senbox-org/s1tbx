/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.util;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.visat.VisatApp;

import java.io.File;
import java.io.IOException;

/**

 */
public class ProductOpener {

        private final VisatApp visatApp;

        public ProductOpener(final VisatApp visatApp) {
            this.visatApp = visatApp;
        }

        public void openProducts(final File[] productFiles) {
            for (File productFile : productFiles) {
                if (!productFile.exists() || isProductOpen(productFile)) {
                    continue;
                }
                try {
                    final Product product = ProductIO.readProduct(productFile);

                    final ProductManager productManager = visatApp.getProductManager();
                    productManager.addProduct(product);
                } catch (IOException e) {
                    visatApp.showErrorDialog("Not able to open product:\n" +
                            productFile.getPath());
                }
            }
        }

        private boolean isProductOpen(final File productFile) {
            final Product openedProduct = visatApp.getOpenProduct(productFile);
            if (openedProduct != null) {
                visatApp.showInfoDialog("Product '" + openedProduct.getName() + "' is already opened.", null);
                return true;
            }
            return false;
        }
    }