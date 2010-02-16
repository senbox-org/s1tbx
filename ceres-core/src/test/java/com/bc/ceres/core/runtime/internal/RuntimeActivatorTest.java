package com.bc.ceres.core.runtime.internal;

import junit.framework.TestCase;

import java.net.URL;
import java.io.IOException;


public class RuntimeActivatorTest extends TestCase {

    public void testSpiConfigurationParsing() throws IOException {
        testSpiConfigurationParsing("/services/com.acme.TestSpi1",
                                    new String[]{"com.foo.TestSpi1Impl"});
        testSpiConfigurationParsing("/services/com.acme.TestSpi2",
                                    new String[]{"com.foo.TestSpi2Impl"});
    }

    private void testSpiConfigurationParsing(String path, String[] expectedClassNames) throws IOException {
        URL resource = getClass().getResource(path);
        assertNotNull(resource);
        String[] classNames = RuntimeActivator.parseSpiConfiguration(resource);
        assertNotNull(classNames);
        assertEquals(expectedClassNames.length, classNames.length);
        for (int i = 0; i < expectedClassNames.length; i++) {
            assertEquals("classNames[" + i + "]", expectedClassNames[i], classNames[0]);
        }
    }

}
