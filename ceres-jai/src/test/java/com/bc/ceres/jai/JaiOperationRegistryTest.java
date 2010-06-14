package com.bc.ceres.jai;

import com.bc.ceres.core.CoreException;
import org.junit.Test;

import javax.media.jai.OperationRegistry;

import static org.junit.Assert.assertNotNull;

public class JaiOperationRegistryTest {

    @Test
    public void testUpdateRegistry() throws CoreException {
        OperationRegistry operationRegistry = new OperationRegistry();
        new JaiOperationRegistrySpi().updateRegistry(operationRegistry);
        assertNotNull(operationRegistry.getDescriptor("rendered", "Reinterpret"));
    }
}
