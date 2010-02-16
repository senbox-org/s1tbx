package org.esa.beam.framework.gpf.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.Window;

/**
 * This trivial implementation of the {@link org.esa.beam.framework.ui.AppContext} class
 * is only for testing.
 */
public class DefaultAppContext implements AppContext {
    private Window applicationWindow;
    private String applicationName;
    private ProductManager productManager;
    private Product selectedProduct;
    private PropertyMap preferences;
    private ProductSceneView selectedSceneView;

    public DefaultAppContext(String applicationName) {
        this(applicationName,
             new JFrame(applicationName),
             new ProductManager(),
             new PropertyMap());        
    }


    public DefaultAppContext(String applicationName,
                             Window applicationWindow,
                             ProductManager productManager,
                             PropertyMap preferences) {
        this.applicationWindow = applicationWindow;
        this.applicationName = applicationName;
        this.productManager = productManager;
        this.preferences = preferences;
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public Window getApplicationWindow() {
        return applicationWindow;
    }

    @Override
    public ApplicationPage getApplicationPage() {
        return null;
    }

    public void setApplicationWindow(Window applicationWindow) {
        this.applicationWindow = applicationWindow;
    }

    @Override
    public PropertyMap getPreferences() {
        return preferences;
    }

    public void setPreferences(PropertyMap preferences) {
        this.preferences = preferences;
    }

    @Override
    public ProductManager getProductManager() {
        return productManager;
    }

    public void setProductManager(ProductManager productManager) {
        this.productManager = productManager;
    }

    @Override
    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product selectedProduct) {
        this.selectedProduct = selectedProduct;
    }

    @Override
    public void handleError(Throwable e) {
        handleError("An error occured:\n" + e.getMessage(), e);
    }

    @Override
    public void handleError(String message, Throwable e) {
        if (e != null) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(getApplicationWindow(), message);
    }

    @Override
    public ProductSceneView getSelectedProductSceneView() {
        return selectedSceneView;
    }

    public void setSelectedSceneView(ProductSceneView selectedView) {
        this.selectedSceneView = selectedView;
    }
}
