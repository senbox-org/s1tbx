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
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.runtime.ProxyConfig;
import com.bc.ceres.core.runtime.RuntimeConfig;

import java.net.URL;
import java.util.logging.Logger;

public class DefaultModuleContext extends ExtensibleObject implements ModuleContext {

    private final ModuleImpl module;

    public DefaultModuleContext(ModuleImpl module) {
        this.module = module;
    }

    @Override
    public RuntimeConfig getRuntimeConfig() {
        return module.getRuntime().getRuntimeConfig();
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public Module[] getModules() {
        return module.getRuntime().getModules();
    }

    @Override
    public Module getModule(long id) {
        return module.getRuntime().getModule(id);
    }

    @Override
    public Logger getLogger() {
        return module.getRuntime().getLogger();
    }

    @Override
    public Module installModule(URL url, ProxyConfig proxyConfig, ProgressMonitor pm) throws CoreException {
        return module.getRuntime().installModule(url, proxyConfig, pm);
    }
}
