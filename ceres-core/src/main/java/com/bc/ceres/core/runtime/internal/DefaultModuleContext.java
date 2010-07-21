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
