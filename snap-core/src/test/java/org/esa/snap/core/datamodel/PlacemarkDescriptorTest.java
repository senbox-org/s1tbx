package org.esa.snap.core.datamodel;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class PlacemarkDescriptorTest {
    @Test
    public void testPinDescriptorIsRegistered() throws Exception {
        assertNotNull(PinDescriptor.getInstance());
        assertEquals(PinDescriptor.class, PinDescriptor.getInstance().getClass());
    }

    @Test
    public void testGcpDescriptorIsRegistered() throws Exception {
        assertNotNull(GcpDescriptor.getInstance());
        assertEquals(GcpDescriptor.class, GcpDescriptor.getInstance().getClass());
    }
}
