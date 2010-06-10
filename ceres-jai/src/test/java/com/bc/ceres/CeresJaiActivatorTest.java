package com.bc.ceres;

import com.bc.ceres.core.CoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CeresJaiActivatorTest {

    private OperationRegistry operationRegistry;

    @Test
    public void testStart() throws CoreException {
        assertNull(JAI.getDefaultInstance().getOperationRegistry().getDescriptor("rendered", "Reinterpret"));
        new CeresJaiActivator().start(null);
        assertNotNull(JAI.getDefaultInstance().getOperationRegistry().getDescriptor("rendered", "Reinterpret"));
    }

    @Before
    public void setNewOperationRegistry() {
        operationRegistry = JAI.getDefaultInstance().getOperationRegistry();
        JAI.getDefaultInstance().setOperationRegistry(new OperationRegistry());
    }

    @After
    public void setOldOperationRegistry() {
        JAI.getDefaultInstance().setOperationRegistry(operationRegistry);
    }
}
