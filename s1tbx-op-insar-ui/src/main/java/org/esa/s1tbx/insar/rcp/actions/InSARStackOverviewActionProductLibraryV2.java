package org.esa.s1tbx.insar.rcp.actions;

import org.esa.s1tbx.insar.rcp.dialogs.InSARStackOverviewDialog;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.product.library.ui.v2.ProductLibraryV2Action;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.ui.v2.thread.ThreadCallback;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.file.Files;

/**
 * Created by jcoravu on 21/7/2020.
 */
public class InSARStackOverviewActionProductLibraryV2 extends ProductLibraryV2Action implements ThreadCallback<Product[]> {

    public InSARStackOverviewActionProductLibraryV2() {
        super("Stack Overview");
    }

    @Override
    public boolean canAddItemToPopupMenu(AbstractProductsRepositoryPanel visibleProductsRepositoryPanel, RepositoryProduct[] selectedProducts) {
        if (selectedProducts.length > 1) {
            boolean allProductsExits = true;
            for (int i=0; i<selectedProducts.length && allProductsExits; i++) {
                if (selectedProducts[i] instanceof LocalRepositoryProduct) {
                    LocalRepositoryProduct localRepositoryProduct = (LocalRepositoryProduct)selectedProducts[i];
                    if (!Files.exists(localRepositoryProduct.getPath())) {
                        allProductsExits = false;
                    }
                } else {
                    allProductsExits = false;
                }
            }
            return allProductsExits;
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        RepositoryProduct[] selectedProducts = this.productLibraryToolView.getSelectedProducts();
        this.productLibraryToolView.readLocalProductsAsync(selectedProducts, this);
    }

    @Override
    public void onFailed(Exception exception) {
        this.productLibraryToolView.showMessageDialog("Stack Overview", "The selected products could not be read from the local repository.", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void onSuccessfullyFinish(Product[] products) {
        InSARStackOverviewDialog dialog = new InSARStackOverviewDialog();
        dialog.setInputProductList(products);
        dialog.show();
    }
}
