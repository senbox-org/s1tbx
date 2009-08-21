package com.bc.ceres.binding;

import junit.framework.TestCase;

public class ValueDescriptorTest extends TestCase {
    public void testMandatoryNameAndType() {
        ValueDescriptor descriptor;

        descriptor = new ValueDescriptor("wasIstDasDenn", String.class);
        assertEquals("wasIstDasDenn", descriptor.getName());
        assertEquals("Was ist das denn", descriptor.getDisplayName());
        assertSame(String.class, descriptor.getType());

        descriptor = new ValueDescriptor("was_ist_das_denn", Double.TYPE);
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
}
