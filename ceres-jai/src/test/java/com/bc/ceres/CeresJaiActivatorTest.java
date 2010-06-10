package com.bc.ceres;

import com.bc.ceres.core.CoreException;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CeresJaiActivatorTest {

    @Test
    public void testStart() throws CoreException {
        assertNull(JAI.getDefaultInstance().getOperationRegistry().getDescriptor("rendered", "Reinterpret"));
        new CeresJaiActivator().start(null);
        assertNotNull(JAI.getDefaultInstance().getOperationRegistry().getDescriptor("rendered", "Reinterpret"));
    }

    @Before
    public void setNewOperatorRegistry() {
        JAI.getDefaultInstance().setOperationRegistry(new OperationRegistry());
    }
}
