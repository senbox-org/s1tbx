package org.esa.s1tbx.insar.rcp.actions;

import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.product.library.ui.v2.ProductLibraryToolViewV2;
import org.esa.snap.product.library.ui.v2.ProductLibraryV2Action;
import org.esa.snap.product.library.ui.v2.repository.AbstractProductsRepositoryPanel;
import org.esa.snap.product.library.v2.database.model.LocalRepositoryProduct;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.awt.event.ActionEvent;
import java.nio.file.Files;

/**
 * Created by jcoravu on 21/7/2020.
 */
public class FindSlicesActionProductLibraryV2 extends ProductLibraryV2Action {

    public FindSlicesActionProductLibraryV2() {
        super("Find Related Slices");
    }

    @Override
    public boolean canAddItemToPopupMenu(AbstractProductsRepositoryPanel visibleProductsRepositoryPanel, RepositoryProduct[] selectedProducts) {
        if (selectedProducts.length == 1) {
            if (selectedProducts[0] instanceof LocalRepositoryProduct) {
                LocalRepositoryProduct localRepositoryProduct = (LocalRepositoryProduct)selectedProducts[0];
                if (Files.exists(localRepositoryProduct.getPath())) {
                    Integer dataTakeId = ProductLibraryToolViewV2.findLocalAttributeAsInt(AbstractMetadata.data_take_id, localRepositoryProduct);
                    if (dataTakeId != null && dataTakeId.intValue() != AbstractMetadata.NO_METADATA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        this.productLibraryToolView.findRelatedSlices();
    }
}
