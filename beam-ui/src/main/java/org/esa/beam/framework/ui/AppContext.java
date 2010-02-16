package org.esa.beam.framework.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;

import java.awt.Window;

public interface AppContext {
    String getApplicationName();

    Window getApplicationWindow();

    ApplicationPage getApplicationPage();

    Product getSelectedProduct();

    @Deprecated
    void handleError(Throwable e);
    
    void handleError(String message, Throwable e);

    PropertyMap getPreferences();

    ProductManager getProductManager();

    ProductSceneView getSelectedProductSceneView();
}
