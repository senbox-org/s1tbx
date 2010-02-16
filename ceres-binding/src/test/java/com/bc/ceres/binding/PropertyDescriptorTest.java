package com.bc.ceres.binding;

import junit.framework.TestCase;

public class PropertyDescriptorTest extends TestCase {
    public void testMandatoryNameAndType() {
        PropertyDescriptor descriptor;

        descriptor = new PropertyDescriptor("wasIstDasDenn", String.class);
        assertEquals("wasIstDasDenn", descriptor.getName());
        assertEquals("Was ist das denn", descriptor.getDisplayName());
        assertSame(String.class, descriptor.getType());

        descriptor = new PropertyDescriptor("was_ist_das_denn", Double.TYPE);
        assertEquals("was_ist_das_denn", descriptor.getName());
        assertEquals("Was ist das denn", descriptor.getDisplayName());
        assertSame(Double.TYPE, descriptor.getType());

        descriptor.setDisplayName("Was denn");
        assertEquals("Was denn", descriptor.getDisplayName());

        try {
            descriptor.setDisplayName(null);
            fail("NPE expected");
        } catch (NullPointerException e) {
            // ok
        }
    }

    public void testThatPrimitiveTypesAreAlwaysNotNull() {
        assertThatPrimitiveTypesAreAlwaysNotNull(Character.TYPE, Character.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Byte.TYPE, Byte.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Short.TYPE, Short.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Integer.TYPE, Integer.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Long.TYPE, Long.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Float.TYPE, Float.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Double.TYPE, Double.class);
        assertThatPrimitiveTypesAreAlwaysNotNull(Void.TYPE, Void.class);
    }

    private static void assertThatPrimitiveTypesAreAlwaysNotNull(Class<?> primitiveType, Class<?> wrapperType) {
        assertEquals(true, new PropertyDescriptor("vd", primitiveType).isNotNull());
        assertEquals(false, new PropertyDescriptor("vd", wrapperType).isNotNull());
    }
}
