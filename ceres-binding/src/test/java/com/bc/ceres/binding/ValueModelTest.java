package com.bc.ceres.binding;

import com.bc.ceres.binding.validators.TypeValidator;
import com.bc.ceres.binding.validators.MultiValidator;
import com.bc.ceres.binding.validators.NotNullValidator;
import junit.framework.TestCase;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;

public class ValueModelTest extends TestCase {
    private static final long LEGAL_PLONG = 999L;
    private static final Long LEGAL_OLONG = 999L;
    private static final String ILLEGAL = "42";
    private static final float FLOAT_EPS = 1.0e-4f;
    private static final double DOUBLE_EPS = 1.0e-10;
    private static final String MODE_ACCURATE = "acc";
    private static final String MODE_FUZZY = "fuz";

    long plong;
    Long olong;

    float pfloat;
    Float ofloat;

    double pdouble;
    Double odouble;

    public void testFactoryUsingClassFieldAccessor() throws ValidationException {
        testPLong(ValueModel.createClassFieldModel(this, "plong"), 0L);
        testOLong(ValueModel.createClassFieldModel(this, "olong"), null);
        assertEquals(LEGAL_PLONG, plong);
        assertEquals(LEGAL_OLONG, olong);
        testPLong(ValueModel.createClassFieldModel(this, "plong", 42L), 42L);
        testOLong(ValueModel.createClassFieldModel(this, "olong", 43L), 43L);
        assertEquals(LEGAL_PLONG, plong);
        assertEquals(LEGAL_OLONG, olong);
    }

    public void testFactoryUsingMapEntryAccessor() throws ValidationException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        testPLong(ValueModel.createMapEntryModel(map, "plong", Long.TYPE), 0L);
        testOLong(ValueModel.createMapEntryModel(map, "olong", Long.class), null);
        assertEquals(LEGAL_OLONG, map.get("plong"));
        assertEquals(LEGAL_OLONG, map.get("olong"));
        testPLong(ValueModel.createMapEntryModel(map, "plong", Long.TYPE, 42L), 42L);
        testOLong(ValueModel.createMapEntryModel(map, "olong", Long.class, 43L), 43L);
        assertEquals(LEGAL_OLONG, map.get("plong"));
        assertEquals(LEGAL_OLONG, map.get("olong"));
    }

    public void testDefaultFactoryWithType() throws ValidationException {
        testPLong(ValueModel.createValueModel("plong", Long.TYPE), 0L);
        testOLong(ValueModel.createValueModel("olong", Long.class), null);
        testOLong(ValueModel.createValueModel("olong", 42L), 42L);
    }

    private void testPLong(ValueModel vm, Object expectedValue) throws ValidationException {
        assertNotNull(vm.getDescriptor());
        assertEquals("plong", vm.getDescriptor().getName());
        assertSame(Long.TYPE, vm.getDescriptor().getType());
        assertEquals(true, vm.getDescriptor().isNotNull());
        assertNotNull(vm.getDescriptor().getConverter());
        assertSame(MultiValidator.class, vm.getValidator().getClass());
        Validator[] validators = ((MultiValidator) vm.getValidator()).getValidators();
        assertEquals(2, validators.length);
        assertSame(NotNullValidator.class, validators[0].getClass());
        assertSame(TypeValidator.class, validators[1].getClass());
        assertTrue(vm.getValue() instanceof Long);
        assertEquals(expectedValue, vm.getValue());
        testSetLegalValue(vm);
        testSetIllegalValue(vm);
    }

    private void testOLong(ValueModel vm, Object expectedValue) throws ValidationException {
        assertNotNull(vm.getDescriptor());
        assertEquals("olong", vm.getDescriptor().getName());
        assertEquals(false, vm.getDescriptor().isNotNull());
        assertSame(Long.class, vm.getDescriptor().getType());
        assertNotNull(vm.getDescriptor().getConverter());
        assertSame(TypeValidator.class, vm.getValidator().getClass());
        assertEquals(expectedValue, vm.getValue());
        testSetLegalValue(vm);
        testSetIllegalValue(vm);
    }

    private void testSetLegalValue(ValueModel vm) throws ValidationException {
        vm.setValue(LEGAL_OLONG);
        assertEquals(LEGAL_OLONG, vm.getValue());
    }

    private void testSetIllegalValue(ValueModel valueModel) {
        try {
            valueModel.setValue(ILLEGAL);
            fail("ValidationException expected");
        } catch (ValidationException e) {
            // ok
        }
    }

    public void testPropertyChangeEvents() throws ValidationException {
        final ValueModel plong = ValueModel.createClassFieldModel(this, "plong");
        final ValueModel olong = ValueModel.createClassFieldModel(this, "olong");
        final ValueModel pfloat = ValueModel.createClassFieldModel(this, "pfloat");
        final ValueModel ofloat = ValueModel.createClassFieldModel(this, "ofloat");
        final ValueModel pdouble = ValueModel.createClassFieldModel(this, "pdouble");
        final ValueModel odouble = ValueModel.createClassFieldModel(this, "odouble");

        final ValueContainer container = new ValueContainer();
        final ValueModelChangeListener listener = new ValueModelChangeListener();
        container.addPropertyChangeListener(listener);
        container.addModel(plong);
        container.addModel(olong);
        container.addModel(pfloat);
        container.addModel(ofloat);
        container.addModel(pdouble);
        container.addModel(odouble);

        testPCLLong(plong, listener);
        testPCLLong(olong, listener);
        testPCLFloat(pfloat, listener, MODE_ACCURATE);
        testPCLFloat(ofloat, listener, MODE_ACCURATE);
        testPCLDouble(pdouble, listener, MODE_ACCURATE);
        testPCLDouble(odouble, listener, MODE_ACCURATE);

        pfloat.getDescriptor().setProperty("eps", FLOAT_EPS);
        ofloat.getDescriptor().setProperty("eps", FLOAT_EPS);
        pdouble.getDescriptor().setProperty("eps", DOUBLE_EPS);
        odouble.getDescriptor().setProperty("eps", DOUBLE_EPS);

        testPCLLong(plong, listener);
        testPCLLong(olong, listener);
        testPCLFloat(pfloat, listener, MODE_FUZZY);
        testPCLFloat(ofloat, listener, MODE_FUZZY);
        testPCLDouble(pdouble, listener, MODE_FUZZY);
        testPCLDouble(odouble, listener, MODE_FUZZY);
    }

    private void testPCLLong(ValueModel model, ValueModelChangeListener listener) throws ValidationException {
        final int n0 = listener.events.size();
        model.setValue(1L);
        assertEquals(1, listener.events.size() - n0);
        model.setValue(1L);
        assertEquals(1, listener.events.size() - n0);
        model.setValue(2L);
        assertEquals(2, listener.events.size() - n0);
    }

    private void testPCLFloat(ValueModel model, ValueModelChangeListener listener, String mode) throws ValidationException {
        final int n0 = listener.events.size();
        model.setValue(1.0f);
        assertEquals(1, listener.events.size() - n0);
        model.setValue(1.0f);
        assertEquals(1, listener.events.size() - n0);
        model.setValue(2.0f);
        assertEquals(2, listener.events.size() - n0);
        if (mode.equals(MODE_FUZZY)) {
            model.setValue(2.0f - 0.99f * FLOAT_EPS);
            assertEquals(2, listener.events.size() - n0);
            model.setValue(2.0f + 0.99f * FLOAT_EPS);
            assertEquals(2, listener.events.size() - n0);
        } else {
            model.setValue(2.0f - 0.99f * FLOAT_EPS);
            assertEquals(3, listener.events.size() - n0);
            model.setValue(2.0f + 0.99f * FLOAT_EPS);
            assertEquals(4, listener.events.size() - n0);
        }
    }

    private void testPCLDouble(ValueModel model, ValueModelChangeListener listener, String mode) throws ValidationException {
        final int n0 = listener.events.size();
        model.setValue(1.0);
        assertEquals(1, listener.events.size() - n0);
        model.setValue(1.0);
        assertEquals(1, listener.events.size() - n0);
        model.setValue(2.0);
        assertEquals(2, listener.events.size() - n0);

        if (mode.equals(MODE_FUZZY)) {
            model.setValue(2.0 - 0.99 * DOUBLE_EPS);
            assertEquals(2, listener.events.size() - n0);
            model.setValue(2.0 + 0.99 * DOUBLE_EPS);
            assertEquals(2, listener.events.size() - n0);
        } else {
            model.setValue(2.0 - 0.99 * DOUBLE_EPS);
            assertEquals(3, listener.events.size() - n0);
            model.setValue(2.0 + 0.99 * DOUBLE_EPS);
            assertEquals(4, listener.events.size() - n0);
        }
    }

    private static class ValueModelChangeListener implements PropertyChangeListener {
        ArrayList<PropertyChangeEvent> events = new ArrayList<PropertyChangeEvent>();

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            events.add(event);
        }
    }
}
