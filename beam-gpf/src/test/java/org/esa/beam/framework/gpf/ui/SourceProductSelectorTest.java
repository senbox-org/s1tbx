package org.esa.beam.framework.gpf.ui;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.PropertyMap;

import javax.swing.JOptionPane;
import java.awt.Window;
import java.io.File;

/**
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SourceProductSelectorTest extends TestCase {

    private Product[] defaultProducts;
    private AppContext appContext;


    @Override
    protected void setUp() throws Exception {
        defaultProducts = new Product[4];
        for (int i = 0; i < defaultProducts.length; i++) {
            defaultProducts[i] = new Product("P" + i, "T" + i, 10, 10);
        }
        appContext = new MockAppContext();

    }

    public void testCreatedUIComponentsNotNull() {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source:");
        selector.initProducts();
        assertNotNull(selector.getProductNameLabel());
        assertNotNull(selector.getProductNameComboBox());
        assertNotNull(selector.getProductFileChooserButton());
    }

    public void testCreatedUIComponentsAreSame() {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source:");
        selector.initProducts();
        assertSame(selector.getProductNameLabel(), selector.getProductNameLabel());
        assertSame(selector.getProductNameComboBox(), selector.getProductNameComboBox());
        assertSame(selector.getProductFileChooserButton(), selector.getProductFileChooserButton());
    }

    public void testSetSelectedProduct() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();
        Product selectedProduct = selector.getSelectedProduct();
        assertNull(selectedProduct);

        selector.setSelectedProduct(defaultProducts[1]);
        selectedProduct = selector.getSelectedProduct();
        assertSame(defaultProducts[1], selectedProduct);

        Product oldProduct = new Product("new", "T1", 0, 0);
        oldProduct.setFileLocation(new File(""));
        selector.setSelectedProduct(oldProduct);
        selectedProduct = selector.getSelectedProduct();
        assertSame(oldProduct, selectedProduct);

        Product newProduct = new Product("new", "T2", 0, 0);
        selector.setSelectedProduct(newProduct);
        selectedProduct = selector.getSelectedProduct();
        assertSame(newProduct, selectedProduct);
        assertNull(oldProduct.getFileLocation()); // assert that old product is disposed
    }

    public void testReleaseProducts() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();
        try {
            selector.releaseProducts();
        } catch (Throwable e) {
            fail("No Throwable expected");
        }


        selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();

        Product newProduct = new Product("new", "T1", 0, 0);
        newProduct.setFileLocation(new File(""));
        selector.setSelectedProduct(newProduct);

        Product selectedProduct = selector.getSelectedProduct();
        assertSame(newProduct, selectedProduct);

        selector.releaseProducts();
        assertNotNull(newProduct.getFileLocation()); // assert that new product is not disposed while it is selected

        selector.setSelectedProduct(defaultProducts[0]);
        selectedProduct = selector.getSelectedProduct();
        assertSame(defaultProducts[0], selectedProduct);

        assertNotNull(newProduct.getFileLocation());
        selector.releaseProducts();
        assertNull(newProduct.getFileLocation()); // assert that new product is disposed
    }

    public void testSetSelectedIndex() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();
        assertNull(selector.getSelectedProduct());

        selector.setSelectedIndex(0);
        assertSame(defaultProducts[0], selector.getSelectedProduct());
    }

    private class MockAppContext implements AppContext {
        private PropertyMap preferences = new PropertyMap();

        public void addProduct(Product product) {
            System.out.println("product added: " + product);
        }

        public Product[] getProducts() {
            return defaultProducts;
        }

        public Product getSelectedProduct() {
            return defaultProducts[0];
        }

        public Window getApplicationWindow() {
            return null;
        }

        public String getApplicationName() {
            return "Killer App";
        }

        public void handleError(Throwable e) {
            JOptionPane.showMessageDialog(getApplicationWindow(), e.getMessage());
        }

        public PropertyMap getPreferences() {
            return preferences;
        }
    }
}
