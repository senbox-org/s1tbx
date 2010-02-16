package com.bc.ceres.core.runtime.internal;

import junit.framework.TestCase;

import java.io.IOException;

import com.bc.ceres.core.CoreException;

public class ModuleUninstallerTest extends TestCase {
    public void testNullArgConvention() throws IOException, CoreException {
        try {
            new ModuleUninstaller(null);
            fail();
        } catch (NullPointerException e) {
        }
    }

}
