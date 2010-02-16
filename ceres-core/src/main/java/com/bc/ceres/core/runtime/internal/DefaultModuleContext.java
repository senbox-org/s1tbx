package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.runtime.RuntimeConfig;
import com.bc.ceres.core.runtime.ProxyConfig;

import java.net.URL;
import java.util.logging.Logger;

public class DefaultModuleContext implements ModuleContext {

    private ModuleImpl module;

    public DefaultModuleContext(ModuleImpl module) {
        this.module = module;
    }

    public <E> E getExtension(Class<E> extensionType) {
        return ExtensionManager.getInstance().getExtension(this, extensionType);
    }

    public RuntimeConfig getRuntimeConfig() {
        return module.getRuntime().getRuntimeConfig();
    }

    public Module getModule() {
        return module;
    }

    public Module[] getModules() {
        return module.getRuntime().getModules();
    }

    public Module getModule(long id) {
        return module.getRuntime().getModule(id);
    }

    public Logger getLogger() {
        return module.getRuntime().getLogger();
    }

    public Module installModule(URL url, ProxyConfig proxyConfig, ProgressMonitor pm) throws CoreException {
        return module.getRuntime().installModule(url, proxyConfig, pm);
    }
}
