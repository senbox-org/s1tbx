package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.junit.*;

import javax.swing.*;

import static org.junit.Assert.*;

public class RoiMaskSelectorTest {

    private RoiMaskSelector roiMaskSelector;
    private BindingContext bindingContext;

    private static class TestModelForBinding {
        private Boolean useRoiMask;
        private Mask selectedRoiMask;
    }

    @Before
    public void setUp() throws Exception {
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(new TestModelForBinding()));
        roiMaskSelector = new RoiMaskSelector(bindingContext);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testThatUIComponentsAreInitialized() {
        assertNotNull(roiMaskSelector.checkBoxUseRoiMask);
        assertEquals(JCheckBox.class, roiMaskSelector.checkBoxUseRoiMask.getClass());
        assertEquals("Use ROI mask", roiMaskSelector.checkBoxUseRoiMask.getText());
        assertFalse(roiMaskSelector.checkBoxUseRoiMask.isSelected());

        assertNotNull(roiMaskSelector.labelRoiMask);
        assertEquals(JLabel.class, roiMaskSelector.labelRoiMask.getClass());
        assertEquals("ROI mask:", roiMaskSelector.labelRoiMask.getText());

        assertNotNull(roiMaskSelector.comboRoiMask);
        assertEquals(JComboBox.class, roiMaskSelector.comboRoiMask.getClass());

        assertNotNull(roiMaskSelector.buttonMaskManager);
        assertEquals(JButton.class, roiMaskSelector.buttonMaskManager.getClass());
        assertNotNull(roiMaskSelector.buttonMaskManager.getIcon());

    }

    @Test
    public void testEnabledStateOfUIComponentsAfterInitializing() {
        assertFalse(roiMaskSelector.checkBoxUseRoiMask.isEnabled());
        assertFalse(roiMaskSelector.labelRoiMask.isEnabled());
        assertFalse(roiMaskSelector.comboRoiMask.isEnabled());
        assertTrue(roiMaskSelector.buttonMaskManager.isEnabled());
    }

    @Test
    public void testEnabledStateWhenProductContainsMasks() {
        roiMaskSelector.updateMaskSource(createProductWithMasks());

        assertTrue(roiMaskSelector.checkBoxUseRoiMask.isEnabled());
        assertFalse(roiMaskSelector.labelRoiMask.isEnabled());
        assertFalse(roiMaskSelector.comboRoiMask.isEnabled());
        assertTrue(roiMaskSelector.buttonMaskManager.isEnabled());
    }

    @Test
    public void testEnabledStateWhenProductContainsMasksAndUseRoiMaskIsChecked() throws ValidationException {
        roiMaskSelector.updateMaskSource(createProductWithMasks());

        bindingContext.getPropertySet().getProperty(RoiMaskSelector.PROPERTY_NAME_USE_ROI_MASK).setValue(Boolean.TRUE);

        assertTrue(roiMaskSelector.checkBoxUseRoiMask.isEnabled());
        assertTrue(roiMaskSelector.labelRoiMask.isEnabled());
        assertTrue(roiMaskSelector.comboRoiMask.isEnabled());
        assertTrue(roiMaskSelector.buttonMaskManager.isEnabled());
    }

    @Test
    public void testEnabledStateWhenProductContainsNoMasks() {
        roiMaskSelector.updateMaskSource(createProduct());

        assertFalse(roiMaskSelector.checkBoxUseRoiMask.isEnabled());
        assertFalse(roiMaskSelector.labelRoiMask.isEnabled());
        assertFalse(roiMaskSelector.comboRoiMask.isEnabled());
        assertTrue(roiMaskSelector.buttonMaskManager.isEnabled());
    }

    @Test
    public void testThatUIComponentsAreBoundToProperties() {
        final Binding useRoiMaskBinding = bindingContext.getBinding(RoiMaskSelector.PROPERTY_NAME_USE_ROI_MASK);
        assertSame(roiMaskSelector.checkBoxUseRoiMask, useRoiMaskBinding.getComponents()[0]);

        final Binding selectedRoiMaskBinding = bindingContext.getBinding(RoiMaskSelector.PROPERTY_NAME_SELECTED_ROI_MASK);
        assertSame(roiMaskSelector.comboRoiMask, selectedRoiMaskBinding.getComponents()[0]);
    }

    private Product createProductWithMasks() {
        final Product product = createProduct();
        product.getMaskGroup().add(new Mask("mask1", 12,12, Mask.BandMathsType.INSTANCE));
        product.getMaskGroup().add(new Mask("mask2", 12,12, Mask.BandMathsType.INSTANCE));
        return product;
    }

    private Product createProduct() {
        return new Product("Masks", "type", 12, 12);
    }
}
