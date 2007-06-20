package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ExtensionPoint;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.runtime.RuntimeRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class RuntimeActivator implements Activator {

    private static RuntimeActivator instance;

    private Map<String, RuntimeRunnable> applications;
    private ModuleContext moduleContext;

    public static RuntimeActivator getInstance() {
        return instance;
    }

    public RuntimeActivator() {
        instance = this;
    }

    public RuntimeRunnable getApplication(String id) {
        return applications.get(id);
    }

    public ModuleContext getModuleContext() {
        return moduleContext;
    }

    public void start(ModuleContext moduleContext) throws CoreException {
        this.moduleContext = moduleContext;
        applications = new HashMap<String, RuntimeRunnable>(3);

        ExtensionPoint extensionPoint = moduleContext.getModule().getExtensionPoint("applications");
        Extension[] extensions = extensionPoint.getExtensions();
        for (int i = 0; i < extensions.length; i++) {
            Extension extension = extensions[i];

            ConfigurationElement[] children = extension.getConfigurationElement().getChildren("application");
            for (ConfigurationElement child : children) {
                String appId = child.getAttribute("id");
                if (appId == null || appId.length() == 0) {
                    moduleContext.getLogger().severe(
                            "Missing identifier for extension " + i + " of extension point [applications].");
                    continue;
                } else if (applications.containsKey(appId)) {
                    moduleContext.getLogger().warning(
                            "Identifier [" + appId + "] is already in use within extension point [applications].");
                }

                try {
                    RuntimeRunnable application = child.createExecutableExtension(RuntimeRunnable.class);
                    applications.put(appId, application);
                    Module declaringModule = extension.getDeclaringModule();
                    String msg = String.format("Application [%s] registered (declared by module [%s]).", appId,
                                               declaringModule.getSymbolicName());
                    moduleContext.getLogger().info(msg);
                } catch (Throwable e) {
                    // todo - better throw CoreException? Or better register error in moduleContext?
                    Module declaringModule = extension.getDeclaringModule();
                    String msg = String.format("Failed to register application [%s] (declared by module [%s]).",
                                               appId, declaringModule.getSymbolicName());
                    moduleContext.getLogger().log(Level.SEVERE, msg, e);
                }
            }

        }
    }

    public void stop(ModuleContext moduleContext) throws CoreException {
        applications.clear();
        applications = null;
    }
}
