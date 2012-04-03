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
        private Mask roiMask;
    }

    @Before
    public void setUp() throws Exception {
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(new TestModelForBinding()));
        roiMaskSelector = new RoiMaskSelector(bindingContext, new JButton("..."));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testThatUIComponentsAreInitialized() {
        assertNotNull(roiMaskSelector.useRoiMaskCheckBox);
        assertEquals(JCheckBox.class, roiMaskSelector.useRoiMaskCheckBox.getClass());
        assertEquals("Use ROI mask:", roiMaskSelector.useRoiMaskCheckBox.getText());
        assertFalse(roiMaskSelector.useRoiMaskCheckBox.isSelected());

        assertNotNull(roiMaskSelector.roiMaskComboBox);
        assertEquals(JComboBox.class, roiMaskSelector.roiMaskComboBox.getClass());

        assertNotNull(roiMaskSelector.showMaskManagerButton);
        assertEquals(JButton.class, roiMaskSelector.showMaskManagerButton.getClass());
    }

    @Test
    public void testEnabledStateOfUIComponentsAfterInitializing() {
        assertFalse(roiMaskSelector.useRoiMaskCheckBox.isEnabled());
        assertFalse(roiMaskSelector.roiMaskComboBox.isEnabled());
        assertTrue(roiMaskSelector.showMaskManagerButton.isEnabled());
    }

    @Test
    public void testEnabledStateWhenProductContainsMasks() {
        roiMaskSelector.updateMaskSource(createProductWithMasks());

        assertTrue(roiMaskSelector.useRoiMaskCheckBox.isEnabled());
        assertFalse(roiMaskSelector.roiMaskComboBox.isEnabled());
        assertTrue(roiMaskSelector.showMaskManagerButton.isEnabled());
    }

    @Test
    public void testEnabledStateWhenProductContainsMasksAndUseRoiMaskIsChecked() throws ValidationException {
        roiMaskSelector.updateMaskSource(createProductWithMasks());

        bindingContext.getPropertySet().getProperty(RoiMaskSelector.PROPERTY_NAME_USE_ROI_MASK).setValue(Boolean.TRUE);

        assertTrue(roiMaskSelector.useRoiMaskCheckBox.isEnabled());
        assertTrue(roiMaskSelector.roiMaskComboBox.isEnabled());
        assertTrue(roiMaskSelector.showMaskManagerButton.isEnabled());
    }

    @Test
    public void testEnabledStateWhenProductContainsNoMasks() {
        roiMaskSelector.updateMaskSource(createProduct());

        assertFalse(roiMaskSelector.useRoiMaskCheckBox.isEnabled());
        assertFalse(roiMaskSelector.roiMaskComboBox.isEnabled());
        assertTrue(roiMaskSelector.showMaskManagerButton.isEnabled());
    }

    @Test
    public void testThatUIComponentsAreBoundToProperties() {
        final Binding useRoiMaskBinding = bindingContext.getBinding(RoiMaskSelector.PROPERTY_NAME_USE_ROI_MASK);
        assertSame(roiMaskSelector.useRoiMaskCheckBox, useRoiMaskBinding.getComponents()[0]);

        final Binding selectedRoiMaskBinding = bindingContext.getBinding(RoiMaskSelector.PROPERTY_NAME_SELECTED_ROI_MASK);
        assertSame(roiMaskSelector.roiMaskComboBox, selectedRoiMaskBinding.getComponents()[0]);
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
