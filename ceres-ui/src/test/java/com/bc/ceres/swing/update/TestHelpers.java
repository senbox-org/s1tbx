package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.ModuleImpl;
import com.bc.ceres.core.runtime.internal.ModuleManifestParser;
import com.bc.ceres.core.CoreException;
import junit.framework.Assert;

import java.io.IOException;
import java.net.URL;

public class TestHelpers {

    public static Module newRepositoryModuleMock(String name, String version, ModuleState state) throws CoreException {
        return newModuleImpl(name, version, state);
    }

    public static ModuleItem newModuleItemMock(String name, String version, ModuleState state) throws CoreException {
        return new ModuleItem(newModuleImpl(name, version, state));
    }

    private static ModuleImpl newModuleImpl(String name, String version, ModuleState state) throws CoreException {
        ModuleImpl module = null;
        String resource = "xml/" + name + ".xml";
        try {
            module = loadModule(resource);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(resource + ": " + e.getMessage());
        }
        module.setVersion(version);
        module.setState(state);
        return module;
    }

    public static ModuleImpl loadModule(String resource) throws IOException, CoreException {
        URL url = ModuleSyncRunnerTest.class.getResource(resource);
        if (url == null) {
            Assert.fail("resource not found: " + resource);
        }
        ModuleImpl module = new ModuleManifestParser().parse(url.openStream());
        module.setLocation(url);
        return module;
    }
}
