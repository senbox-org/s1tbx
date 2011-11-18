package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import org.junit.Test;

/**
 * @author Norman Fomferra
 */
public class ValueSetValidatorTest {

    @Test
    public void testGoodNull() throws Exception {
        final PropertyDescriptor descriptor = createPropertyDescriptor();
        descriptor.setNotNull(false);
        new ValueSetValidator(descriptor).validateValue(createProperty(descriptor), null);
    }

    @Test(expected = ValidationException.class)
    public void testBadNull() throws Exception {
        final PropertyDescriptor descriptor = createPropertyDescriptor();
        descriptor.setNotNull(true);
        new ValueSetValidator(descriptor).validateValue(createProperty(descriptor), null);
    }

    @Test
    public void testGoodValues() throws Exception {
        final PropertyDescriptor descriptor = createPropertyDescriptor();
        descriptor.setNotNull(false);
        final ValueSetValidator validator = new ValueSetValidator(descriptor);
        validator.validateValue(createProperty(descriptor), "a");
        validator.validateValue(createProperty(descriptor), "b");
        validator.validateValue(createProperty(descriptor), "c");
    }

    @Test(expected = ValidationException.class)
    public void testBadValue() throws Exception {
        final PropertyDescriptor descriptor = createPropertyDescriptor();
        descriptor.setNotNull(false);
        final ValueSetValidator validator = new ValueSetValidator(descriptor);
        validator.validateValue(createProperty(descriptor), "d");
    }

    private Property createProperty(PropertyDescriptor descriptor) {
        return new Property(descriptor, new DefaultPropertyAccessor());
    }

    private PropertyDescriptor createPropertyDescriptor() {
        final PropertyDescriptor descriptor = new PropertyDescriptor("x", String.class);
        descriptor.setValueSet(new ValueSet(new Object[]{"a", "b", "c"}));
        return descriptor;
    }
}
