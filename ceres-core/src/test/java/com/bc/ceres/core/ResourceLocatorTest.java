package com.bc.ceres.core;

import org.junit.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResourceLocatorTest {
    @Test
    public void testGetResources() throws Exception {
        Collection<Path> resources = ResourceLocator.getResources("META-INF/MANIFEST.MF");
        assertNotNull(resources);
        int size = resources.size();
        assertTrue(size > 5);
        for (Path resource : resources) {
            //System.out.println("path = " + path);
            assertNotNull(resource);
            assertTrue(Files.exists(resource));
        }
    }
}
