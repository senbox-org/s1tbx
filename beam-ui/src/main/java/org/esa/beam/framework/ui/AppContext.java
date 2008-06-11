package org.esa.beam.framework.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.util.PropertyMap;

import java.awt.Window;

public interface AppContext {
    String getApplicationName();

    Window getApplicationWindow();

    Product getSelectedProduct();

    void handleError(Throwable e);
    
    PropertyMap getPreferences();

    ProductManager getProductManager();
}
