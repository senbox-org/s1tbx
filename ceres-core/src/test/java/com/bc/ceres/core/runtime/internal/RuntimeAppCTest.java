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

import com.acme.ext.StringExtension1;
import com.acme.ext.StringExtension2;
import com.acme.ext.StringExtension3;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.RuntimeConfigException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class RuntimeAppCTest {
    private RuntimeImpl runtime;

    @Before
    public void setUp() throws CoreException, RuntimeConfigException {
        System.setProperty("ceres.context", "appC");
        System.setProperty("appC.home", Config.getDirForAppC().toString());
        DefaultRuntimeConfig defaultRuntimeConfig = new DefaultRuntimeConfig();
        runtime = new RuntimeImpl(defaultRuntimeConfig, new String[0], ProgressMonitor.NULL);
        runtime.start();
    }

    @After
    public void tearDown() throws Exception {
        runtime.stop();
        runtime = null;
    }

    @Test
    public void testAdaptersAreRegistered() {
        ExtensionFactory[] extensionFactories = ExtensionManager.getInstance().getExtensionFactories(String.class);
        assertNotNull(extensionFactories);
        assertEquals(3, extensionFactories.length);

        StringExtension1 ext1 = ExtensionManager.getInstance().getExtension("EXT-1", StringExtension1.class);
        assertNotNull(ext1);
        assertEquals("EXT-1", ext1.string);

        StringExtension2 ext2 = ExtensionManager.getInstance().getExtension("EXT-2", StringExtension2.class);
        assertNotNull(ext2);

        StringExtension3 ext3 = ExtensionManager.getInstance().getExtension("EXT-3", StringExtension3.class);
        assertNotNull(ext3);
        assertEquals("EXT-3", ext3.string);
    }
}
