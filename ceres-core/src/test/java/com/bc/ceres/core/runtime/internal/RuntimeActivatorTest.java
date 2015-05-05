/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.core.runtime.internal;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;


public class RuntimeActivatorTest {

    @Test
    public void testSpiConfigurationParsing() throws IOException {
        testSpiConfigurationParsing("/services/com.acme.TestSpi1", new String[]{"com.foo.TestSpi1Impl"});
        testSpiConfigurationParsing("/services/com.acme.TestSpi2", new String[]{"com.foo.TestSpi2Impl"});
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
