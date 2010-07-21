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
 * A strategy which starts modules.
 */
public class ModuleStarter {

    private ModuleImpl module;

    public ModuleStarter(ModuleImpl module) {
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    public void run() throws CoreException {
        runImpl(module);
    }

    private static void runImpl(ModuleImpl module) throws CoreException {

        if (module.getState() != ModuleState.STARTING
            && module.getState() != ModuleState.ACTIVE) {

            if (module.getState() != ModuleState.RESOLVED) {
                throw new CoreException(String.format("Failed to start module [%s], RESOLVED state required",
                                                      module.getSymbolicName()));
            }

            module.setState(ModuleState.STARTING);

            ModuleContext context = new DefaultModuleContext(module);
            module.setContext(context);

            Activator activator;
            try {
                activator = createActivator(module);
            } catch (Throwable t) {
                module.setState(ModuleState.RESOLVED);
                throw new CoreException(t);
            }
            module.setActivator(activator);

            try {
                // note: executing foreign code here!
                activator.start(context);
            } catch (Throwable t) {
                module.setState(ModuleState.RESOLVED);
                throw new CoreException(t);
            }

            module.setState(ModuleState.ACTIVE);
        }
    }

    public static Activator createActivator(ModuleImpl module) throws Exception {
        Activator activator;
        if (module.getActivatorClassName() != null) {
            Class<?> aClass = module.loadClass(module.getActivatorClassName());
            // note: executing foreign code here!
            activator = (Activator) aClass.newInstance();
        } else {
            activator = new DefaultActivator();
        }
        return activator;
    }
}
