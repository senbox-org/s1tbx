package org.esa.beam.collocation.visat;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.dataop.resamp.Resampling;

import javax.swing.*;

/**
 * Tests for class {@link CollocationFormModel}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class CollocationFormModelTest extends TestCase {

    private CollocationFormModel collocationFormModel;
    private ComboBoxModel resamplingComboBoxModel;

    @Override
    protected void setUp() throws Exception {
        collocationFormModel = new CollocationFormModel();
        resamplingComboBoxModel = collocationFormModel.getResamplingComboBoxModel();
    }

    @Override
    protected void tearDown() throws Exception {
        collocationFormModel = null;
    }

    public void testSetMasterProduct() {
        final Product product = new Product("name", "type", 10, 10);

        collocationFormModel.setMasterProduct(product);
        assertSame(product, collocationFormModel.getMasterProduct());
    }

    public void testSetSlaveProduct() {
        final Product product = new Product("name", "type", 10, 10);

        collocationFormModel.setMasterProduct(product);
        assertSame(product, collocationFormModel.getMasterProduct());
    }

    public void testSetCreateNewProduct() {
        collocationFormModel.setCreateNewProduct(true);
        assertTrue(collocationFormModel.isCreateNewProductSelected());

        collocationFormModel.setCreateNewProduct(false);
        assertFalse(collocationFormModel.isCreateNewProductSelected());
    }

    public void testSetRenameMasterComponents() {
        collocationFormModel.setRenameMasterComponents(true);
        assertTrue(collocationFormModel.isRenameMasterComponentsSelected());

        collocationFormModel.setRenameMasterComponents(false);
        assertFalse(collocationFormModel.isRenameMasterComponentsSelected());
    }

    public void testSetRenameSlaveComponents() {
        collocationFormModel.setRenameSlaveComponents(true);
        assertTrue(collocationFormModel.isRenameSlaveComponentsSelected());

        collocationFormModel.setRenameSlaveComponents(false);
        assertFalse(collocationFormModel.isRenameSlaveComponentsSelected());
    }

    public void testSetMasterComponentPattern() {
        final String pattern = "master_${component_name}";

        collocationFormModel.setMasterComponentPattern(pattern);
        assertEquals(pattern, collocationFormModel.getMasterComponentPattern());
    }

    public void testSetSlaveComponentPattern() {
        final String pattern = "slave_${component_name}";

        collocationFormModel.setSlaveComponentPattern(pattern);
        assertEquals(pattern, collocationFormModel.getSlaveComponentPattern());
    }

    public void testSetResampling() {
        collocationFormModel.setResampling(Resampling.BILINEAR_INTERPOLATION);
        assertEquals(Resampling.BILINEAR_INTERPOLATION, collocationFormModel.getResampling());

        collocationFormModel.setResampling(Resampling.CUBIC_CONVOLUTION);
        assertEquals(Resampling.CUBIC_CONVOLUTION, collocationFormModel.getResampling());

        collocationFormModel.setResampling(Resampling.NEAREST_NEIGHBOUR);
        assertEquals(Resampling.NEAREST_NEIGHBOUR, collocationFormModel.getResampling());
    }

    public void testAdaptResamplingComboBoxModel() {
        final Product product = new Product("name", "type", 10, 10);
        final Band band1 = product.addBand("band1", ProductData.TYPE_INT32);
        final Band band2 = product.addBand("band2", ProductData.TYPE_INT32);

        collocationFormModel.setSlaveProduct(product);
        collocationFormModel.adaptResamplingComboBoxModel();
        assertEquals(3, resamplingComboBoxModel.getSize());

        band1.setValidPixelExpression("true");
        collocationFormModel.adaptResamplingComboBoxModel();
        assertEquals(1, resamplingComboBoxModel.getSize());
        assertEquals(Resampling.NEAREST_NEIGHBOUR, collocationFormModel.getResampling());

        band1.setValidPixelExpression(null);
        band2.setValidPixelExpression("  ");
        collocationFormModel.adaptResamplingComboBoxModel();
        assertEquals(3, resamplingComboBoxModel.getSize());
        assertEquals(Resampling.NEAREST_NEIGHBOUR, collocationFormModel.getResampling());
    }
}
