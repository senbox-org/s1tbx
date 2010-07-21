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
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.runtime.ModuleState;

/**
 * A strategy which stops modules.
 */
public class ModuleStopper {
    private ModuleImpl module;

    public ModuleStopper(ModuleImpl module) {
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    public void run() throws CoreException {
        ModuleStopper.runImpl(module);
    }

    public static void runImpl(ModuleImpl module) throws CoreException {

        if (module.getState() != ModuleState.STOPPING
                && module.getState() != ModuleState.RESOLVED) {

            if (module.getState() != ModuleState.ACTIVE) {
                throw new CoreException(String.format("Failed to stop module [%s], ACTIVE state required", module.getSymbolicName()));
            }

            module.setState(ModuleState.STOPPING);

            ModuleContext context = module.getContext();
            Activator activator = module.getActivator();
            try {
                // note - executing foreign code here!
                activator.stop(context);
            } catch (Throwable t) {
                module.setState(ModuleState.ACTIVE);
                throw new CoreException(t);
            }

            module.setActivator(null);
            module.setContext(null);
            module.setState(ModuleState.RESOLVED);
        }

    }
}
