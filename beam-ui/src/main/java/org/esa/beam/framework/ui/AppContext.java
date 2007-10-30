package org.esa.beam.framework.ui;

import org.esa.beam.framework.datamodel.Product;

import java.awt.Window;

public interface AppContext {
    String getApplicationName();

    Window getApplicationWindow();

    Product[] getProducts();

    Product getSelectedProduct();

    void addProduct(Product product);

    void handleError(Throwable e);
}
