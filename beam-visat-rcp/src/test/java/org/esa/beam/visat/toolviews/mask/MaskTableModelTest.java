package org.esa.beam.visat.toolviews.mask;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;


public class MaskTableModelTest extends TestCase {
    public void testManagementMode() {
        MaskTableModel maskTableModel = new MaskTableModel(true);
        assertEquals(false, maskTableModel.isInManagmentMode());
        assertEquals(5, maskTableModel.getColumnCount());
        assertEquals("Name", maskTableModel.getColumnName(0));
        assertEquals("Type", maskTableModel.getColumnName(1));
        assertEquals("Colour", maskTableModel.getColumnName(2));
        assertEquals("Transparency", maskTableModel.getColumnName(3));
        assertEquals("Description", maskTableModel.getColumnName(4));
        assertEquals(0, maskTableModel.getRowCount());

        Product product = MaskFormTest.createTestProduct();
        maskTableModel.setProduct(product, null);
        assertEquals(true, maskTableModel.isInManagmentMode());
        assertEquals(5, maskTableModel.getColumnCount());
        assertEquals("Name", maskTableModel.getColumnName(0));
        assertEquals("Type", maskTableModel.getColumnName(1));
        assertEquals("Colour", maskTableModel.getColumnName(2));
        assertEquals("Transparency", maskTableModel.getColumnName(3));
        assertEquals("Description", maskTableModel.getColumnName(4));
        assertEquals(14, maskTableModel.getRowCount());

        maskTableModel.setProduct(product, product.getBand("C"));
        assertEquals(true, maskTableModel.isInManagmentMode());
        assertEquals(7, maskTableModel.getColumnCount());
        assertEquals("Visibility", maskTableModel.getColumnName(0));
        assertEquals("ROI", maskTableModel.getColumnName(1));
        assertEquals("Name", maskTableModel.getColumnName(2));
        assertEquals("Type", maskTableModel.getColumnName(3));
        assertEquals("Colour", maskTableModel.getColumnName(4));
        assertEquals("Transparency", maskTableModel.getColumnName(5));
        assertEquals("Description", maskTableModel.getColumnName(6));
        assertEquals(14, maskTableModel.getRowCount());
    }

    public void testViewMode() {
        MaskTableModel maskTableModel = new MaskTableModel(false);
        assertEquals(false, maskTableModel.isInManagmentMode());
        assertEquals(4, maskTableModel.getColumnCount());
        assertEquals("Name", maskTableModel.getColumnName(0));
        assertEquals("Colour", maskTableModel.getColumnName(1));
        assertEquals("Transparency", maskTableModel.getColumnName(2));
        assertEquals("Description", maskTableModel.getColumnName(3));
        assertEquals(0, maskTableModel.getRowCount());

        Product product = MaskFormTest.createTestProduct();
        maskTableModel.setProduct(product, null);
        assertEquals(false, maskTableModel.isInManagmentMode());
        assertEquals(4, maskTableModel.getColumnCount());
        assertEquals("Name", maskTableModel.getColumnName(0));
        assertEquals("Colour", maskTableModel.getColumnName(1));
        assertEquals("Transparency", maskTableModel.getColumnName(2));
        assertEquals("Description", maskTableModel.getColumnName(3));
        assertEquals(14, maskTableModel.getRowCount());

        maskTableModel.setProduct(product, product.getBand("C"));
        assertEquals(false, maskTableModel.isInManagmentMode());
        assertEquals(5, maskTableModel.getColumnCount());
        assertEquals("Visibility", maskTableModel.getColumnName(0));
        assertEquals("Name", maskTableModel.getColumnName(1));
        assertEquals("Colour", maskTableModel.getColumnName(2));
        assertEquals("Transparency", maskTableModel.getColumnName(3));
        assertEquals("Description", maskTableModel.getColumnName(4));
        assertEquals(14, maskTableModel.getRowCount());
    }
}
