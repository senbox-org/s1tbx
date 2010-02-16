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
