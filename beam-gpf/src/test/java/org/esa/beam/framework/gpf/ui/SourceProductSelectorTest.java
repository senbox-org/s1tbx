package org.esa.beam.framework.gpf.ui;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;

import java.io.File;

/**
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SourceProductSelectorTest extends TestCase {

    private Product[] defaultProducts;
    private DefaultAppContext appContext;


    @Override
    protected void setUp() throws Exception {
        appContext = new DefaultAppContext("Fart, fart!");
        defaultProducts = new Product[4];
        for (int i = 0; i < defaultProducts.length; i++) {
            defaultProducts[i] = new Product("P" + i, "T" + i, 10, 10);
            appContext.getProductManager().addProduct(defaultProducts[i]);
        }
        appContext.setSelectedProduct(defaultProducts[0]);
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
        assertSame(appContext.getSelectedProduct(), selectedProduct);

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

    public void testNewProductIsDisposed() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();
        Product newProduct = new Product("new", "T1", 0, 0);
        newProduct.setFileLocation(new File(""));
        selector.setSelectedProduct(newProduct);
        assertSame(newProduct, selector.getSelectedProduct());
        selector.setSelectedProduct(defaultProducts[0]);
        assertSame(defaultProducts[0], selector.getSelectedProduct());

        assertNotNull(newProduct.getFileLocation());
        selector.releaseProducts();
        assertNull(newProduct.getFileLocation()); // assert that new product is disposed, because it is not selected
    }

    public void testNewProductIsNotDisposed() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");
        selector.initProducts();
        selector.setSelectedProduct(defaultProducts[0]);
        assertSame(defaultProducts[0], selector.getSelectedProduct());
        Product newProduct = new Product("new", "T1", 0, 0);
        newProduct.setFileLocation(new File(""));
        selector.setSelectedProduct(newProduct);
        assertSame(newProduct, selector.getSelectedProduct());

        assertNotNull(newProduct.getFileLocation());
        selector.releaseProducts();
        assertNotNull(newProduct.getFileLocation()); // assert that new product is not disposed while it is selected
    }

    public void testSetSelectedIndex() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(appContext, "Source");

        selector.initProducts();
        assertSame(defaultProducts[0], selector.getSelectedProduct());

        selector.setSelectedIndex(1);
        assertSame(defaultProducts[1], selector.getSelectedProduct());

        selector.setSelectedIndex(2);
        assertSame(defaultProducts[2], selector.getSelectedProduct());
    }

}
