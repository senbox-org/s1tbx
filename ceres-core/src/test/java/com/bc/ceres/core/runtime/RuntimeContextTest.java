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

package com.bc.ceres.core.runtime;

import junit.framework.TestCase;
import com.bc.ceres.core.runtime.internal.Config;
import com.bc.ceres.core.runtime.internal.DefaultRuntimeConfig;
import com.bc.ceres.core.runtime.internal.RuntimeImpl;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.CoreException;

public class RuntimeContextTest extends TestCase {
    public void testRuntime() throws RuntimeConfigException, CoreException {
        assertFalse(RuntimeContext.isAvailable());
        assertNull(RuntimeContext.getConfig());
        assertNull(RuntimeContext.getModuleContext());

        System.setProperty("ceres.context", "appA");
        System.setProperty("appA.home", Config.getDirForAppA().toString());
        DefaultRuntimeConfig defaultRuntimeConfig = new DefaultRuntimeConfig();
        RuntimeImpl runtime = new RuntimeImpl(defaultRuntimeConfig, new String[0], ProgressMonitor.NULL);
        runtime.start();

        assertTrue(RuntimeContext.isAvailable());
        assertNotNull(RuntimeContext.getConfig());
        assertNotNull(RuntimeContext.getModuleContext());

        runtime.stop();

        assertFalse(RuntimeContext.isAvailable());
        assertNull(RuntimeContext.getConfig());
        assertNull(RuntimeContext.getModuleContext());
    }
}
