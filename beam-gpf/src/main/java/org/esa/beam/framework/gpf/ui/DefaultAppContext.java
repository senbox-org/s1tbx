package org.esa.beam.framework.gpf.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.PropertyMap;

import javax.swing.JOptionPane;
import javax.swing.JFrame;
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

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Window getApplicationWindow() {
        return applicationWindow;
    }

    public void setApplicationWindow(Window applicationWindow) {
        this.applicationWindow = applicationWindow;
    }

    public PropertyMap getPreferences() {
        return preferences;
    }

    public void setPreferences(PropertyMap preferences) {
        this.preferences = preferences;
    }

    public ProductManager getProductManager() {
        return productManager;
    }

    public void setProductManager(ProductManager productManager) {
        this.productManager = productManager;
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product selectedProduct) {
        this.selectedProduct = selectedProduct;
    }

    public void handleError(Throwable e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(getApplicationWindow(), e.getMessage());
    }

}
