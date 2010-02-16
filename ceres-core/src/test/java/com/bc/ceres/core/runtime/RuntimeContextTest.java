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
