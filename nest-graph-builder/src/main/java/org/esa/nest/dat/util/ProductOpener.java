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