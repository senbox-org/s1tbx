package com.bc.ceres.binding;

import junit.framework.TestCase;

import java.util.HashMap;

import com.bc.ceres.binding.validators.TypeValidator;

public class ValueModelTest extends TestCase {
    private static final long LEGAL_PLONG = 999L;
    private static final Long LEGAL_OLONG = 999L;
    private static final String ILLEGAL = "42";

    long plong;
    Long olong;

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
        assertNotNull(vm.getDescriptor().getConverter());
        assertTrue(vm.getValidator() instanceof TypeValidator);
        assertTrue(vm.getValue() instanceof Long);
        assertEquals(expectedValue, vm.getValue());
        testSetLegalValue(vm);
        testSetIllegalValue(vm);
    }

    private void testOLong(ValueModel vm, Object expectedValue) throws ValidationException {
        assertNotNull(vm.getDescriptor());
        assertEquals("olong", vm.getDescriptor().getName());
        assertSame(Long.class, vm.getDescriptor().getType());
        assertNotNull(vm.getDescriptor().getConverter());
        assertTrue(vm.getValidator() instanceof TypeValidator);
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
}
