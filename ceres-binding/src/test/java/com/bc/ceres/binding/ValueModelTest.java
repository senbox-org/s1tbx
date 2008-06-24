package com.bc.ceres.binding;

import junit.framework.TestCase;

import java.util.HashMap;

public class ValueModelTest extends TestCase {
    private static final long LEGAL_PLONG = 999L;
    private static final Long LEGAL_OLONG = 999L;
    private static final String ILLEGAL = "42";

    long plong;
    Long olong;

    public void testFactoryUsingClassFieldAccessor() throws ValidationException {
        testPLong(ValueModel.create(this, "plong"));
        testOLong(ValueModel.create(this, "olong"));
        assertEquals(LEGAL_PLONG, plong);
        assertEquals(LEGAL_OLONG, olong);
    }

    public void testFactoryUsingMapEntryAccessor() throws ValidationException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        testPLong(ValueModel.create(map, "plong", Long.TYPE));
        testOLong(ValueModel.create(map, "olong", Long.class));
        assertEquals(LEGAL_OLONG, map.get("plong"));
        assertEquals(LEGAL_OLONG, map.get("olong"));
    }

    public void testFactoryUsingDefaultAccessor() throws ValidationException {
        testPLong(ValueModel.create("plong", Long.TYPE));
        testOLong(ValueModel.create("olong", Long.class));
    }

    private void testPLong(ValueModel vm) throws ValidationException {
        assertNotNull(vm.getDescriptor());
        assertEquals("plong", vm.getDescriptor().getName());
        assertSame(Long.TYPE, vm.getDescriptor().getType());
        assertTrue(vm.getValue() instanceof Long);
        assertEquals(0L, vm.getValue());
        testSetLegalValue(vm);
        testSetIllegalValue(vm);
    }

    private void testOLong(ValueModel vm) throws ValidationException {
        assertNotNull(vm.getDescriptor());
        assertEquals("olong", vm.getDescriptor().getName());
        assertSame(Long.class, vm.getDescriptor().getType());
        assertEquals(null, vm.getValue());
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
