package org.esa.beam.framework.ui.io;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;

import javax.swing.JLabel;
import java.io.File;

/**
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SourceProductSelectorTest extends TestCase {

    private Product[] defaultProducts;


    @Override
    protected void setUp() throws Exception {
        defaultProducts = new Product[4];
        for (int i = 0; i < defaultProducts.length; i++) {
            defaultProducts[i] = new Product("P" + i, "T" + i, 10, 10);
        }

    }

    public void testCreatedUIComponentsNotNull() {
        SourceProductSelector selector = new SourceProductSelector(defaultProducts, "Source:");
        assertNotNull(selector.getLabel());
        assertNotNull(selector.getComboBox());
        assertNotNull(selector.getButton());
    }

    public void testCreatedUIComponentsAreSame() {
        SourceProductSelector selector = new SourceProductSelector(defaultProducts, "Source:");
        assertSame(selector.getLabel(), selector.getLabel());
        assertSame(selector.getComboBox(), selector.getComboBox());
        assertSame(selector.getButton(), selector.getButton());
    }

    public void testLabelEndsWithColon() {
        JLabel productLabel1 = new SourceProductSelector(defaultProducts, "Source").getLabel();
        assertEquals("Source:", productLabel1.getText());

        JLabel productLabel2 = new SourceProductSelector(defaultProducts, "Source:").getLabel();
        assertEquals("Source:", productLabel2.getText());
    }

    public void testSetSelectedProduct() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(defaultProducts, "Source");

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

    public void testSetSelectedProductThrowsException() {
        SourceProductSelector selector = new SourceProductSelector(defaultProducts, "Source", "T.");

        Product newProduct = new Product("new", "T1", 0, 0);
        try {
            selector.setSelectedProduct(newProduct);
        } catch (Exception e) {
            fail("Exception not expected");
        }
        Product selectedProduct = selector.getSelectedProduct();
        newProduct.setFileLocation(new File(""));
        assertSame(newProduct, selectedProduct);

        Product otherProduct = new Product("other", "P1", 0, 0);
        try {
            selector.setSelectedProduct(otherProduct);
            fail("No exception expected");
        } catch (Exception expected) {
            // ignore
        }

        selectedProduct = selector.getSelectedProduct();
        assertSame(newProduct, selectedProduct);
        assertNotNull(newProduct.getFileLocation()); // assert that old product is not disposed
    }

    public void testDispose() throws Exception {
        SourceProductSelector selector = new SourceProductSelector(defaultProducts, "Source");
        try {
            selector.dispose();
        } catch (Throwable e) {
            fail("No Throwable expected");
        }

        
        selector = new SourceProductSelector(defaultProducts, "Source");

        Product newProduct = new Product("new", "T1", 0, 0);
        newProduct.setFileLocation(new File(""));
        selector.setSelectedProduct(newProduct);

        Product selectedProduct = selector.getSelectedProduct();
        assertSame(newProduct, selectedProduct);

        selector.dispose();
        assertNotNull(newProduct.getFileLocation()); // assert that new product is not disposed while it is selected

        selector.setSelectedProduct(defaultProducts[0]);
        selectedProduct = selector.getSelectedProduct();
        assertSame(defaultProducts[0], selectedProduct);

        assertNotNull(newProduct.getFileLocation());
        selector.dispose();
        assertNull(newProduct.getFileLocation()); // assert that new product is disposed
    }

    public void testFileChooserAction() {
        
    }
}
