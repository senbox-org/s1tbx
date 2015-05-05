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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleRuntime;
import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.ProxyConfig;
import com.bc.ceres.core.runtime.RuntimeConfig;
import com.bc.ceres.core.runtime.RuntimeRunnable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.bc.ceres.core.runtime.Constants.*;

// todo - handle "note: executing foreign code here!"
// todo - delegate specific behaviour to kind-of RuntimeAdvisor / RuntimeConfigurer
// todo - propagate or provide errors/warnings detected during start-up to client

public class RuntimeImpl extends ExtensibleObject implements ModuleRuntime {

    public static final String UNINSTALL_FILE_SUFFIX = ".uninstall";

    private final RuntimeConfig config;
    private final String[] commandLineArgs;
    private final ProgressMonitor progressMonitor;

    private ModuleImpl systemModule;
    private ModuleRegistry moduleRegistry;
    private ArrayList<ModuleImpl> resolvedModules;
    private boolean running;
    private long lastModuleId = 0L;

    public RuntimeImpl(RuntimeConfig config, String[] commandLineArgs, ProgressMonitor progressMonitor) {
        Assert.notNull(config, "config");
        Assert.notNull(commandLineArgs, "commandLineArgs");
        Assert.notNull(progressMonitor, "progressMonitor");
        this.config = config;
        this.commandLineArgs = commandLineArgs;
        this.progressMonitor = progressMonitor;
    }

    public RuntimeConfig getRuntimeConfig() {
        return config;
    }

    public String[] getCommandLineArgs() {
        return commandLineArgs;
    }

    public Logger getLogger() {
        return config.getLogger();
    }

    public Module getModule() {
        return systemModule;
    }

    public Module getModule(long id) {
        return moduleRegistry.getModule(id);
    }

    public Module[] getModules() {
        return moduleRegistry.getModules();
    }

    public Module installModule(URL url, ProxyConfig proxyConfig, ProgressMonitor pm) throws CoreException {
        String modulesDirPath = config.getModulesDirPath();
        if (modulesDirPath == null) {
            throw new CoreException("Modules directory not set");
        }
        File modulesDir = new File(modulesDirPath);
        ModuleInstaller moduleInstaller = new ModuleInstaller(getLogger());
        ModuleImpl module = moduleInstaller.installModule(url, proxyConfig, modulesDir, pm);
        registerModule(module, newModuleId());
        return module;
    }

    public void start() throws CoreException {
        if (running) {
            throw new CoreException("Already running");
        }
        running = true;

        progressMonitor.beginTask("Starting runtime", 100);
        getLogger().info(MessageFormat.format("Starting runtime for context [{0}]...", getContextId()));
        logRuntimeConfig();

        try {
            progressMonitor.setSubTaskName("Loading modules");
            loadModules(SubProgressMonitor.create(progressMonitor, 10));
            initSystemModule();
            progressMonitor.worked(5); // = 5%

            progressMonitor.setSubTaskName("Resolving modules");
            resolveModules(SubProgressMonitor.create(progressMonitor, 5));  // = 15%

            progressMonitor.setSubTaskName("Starting modules");
            startModules(SubProgressMonitor.create(progressMonitor, 55)); // = 70%
            registerShutdownHook();

            progressMonitor.setSubTaskName("Running application");
            runApplication(SubProgressMonitor.create(progressMonitor, 30)); // = 100%
        } finally {
            progressMonitor.done();
        }
    }

    public void stop() throws CoreException {
        if (!running) {
            return;
        }

        getLogger().info(MessageFormat.format("Stopping runtime for context [{0}]...", getContextId()));

        stopModules();
        dispose();
        running = false;
    }

    private String getContextId() {
        return config.getContextId();
    }


    private void dispose() {
        systemModule = null;
        moduleRegistry = null;
        resolvedModules = null;
    }

    private void logRuntimeConfig() {
        getLogger().info("Runtime configuration:");
        getLogger().info(String.format("  contextId      = %s", config.getContextId()));
        getLogger().info(String.format("  homeDirPath    = %s", config.getHomeDirPath()));
        getLogger().info(String.format("  configFilePath = %s", config.getConfigFilePath()));
        getLogger().info(String.format("  modulesDirPath = %s", config.getModulesDirPath()));
        String[] libDirPaths = config.getLibDirPaths();
        for (int i = 0; i < libDirPaths.length; i++) {
            getLogger().info(String.format("  libDirPaths.%d = %s", i, config.getLibDirPaths()[i]));
        }
        getLogger().info(String.format("  mainClassName  = %s", config.getMainClassName()));
        getLogger().info(String.format("  applicationId  = %s", config.getApplicationId()));
    }

    private void loadModules(ProgressMonitor pm) throws CoreException {
        pm.beginTask("Loading modules", 3);
        try {
            if (config.getModulesDirPath() != null) {
                uninstallModules(SubProgressMonitor.create(pm, 1));
            } else {
                pm.worked(1);
            }

            moduleRegistry = new ModuleRegistry();

            ModuleLoader moduleLoader = new ModuleLoader(getLogger());

            if (config.getModulesDirPath() != null) {
                loadModulesFromModulesDir(moduleLoader, SubProgressMonitor.create(pm, 1));
            } else {
                pm.worked(1);
            }

            loadModulesFromClasspath(moduleLoader, SubProgressMonitor.create(pm, 1));

        } finally {
            pm.done();
        }
    }

    private void uninstallModules(ProgressMonitor pm) {
        ModuleUninstaller moduleUninstaller = new ModuleUninstaller(getLogger());
        moduleUninstaller.uninstallModules(new File(config.getModulesDirPath()), pm);
    }

    private void loadModulesFromModulesDir(ModuleLoader moduleLoader, ProgressMonitor pm) throws CoreException {
        File modulesDir = new File(config.getModulesDirPath());
        if (modulesDir.isDirectory()) {
            getLogger().info(String.format("Searching for modules in [%s]...", modulesDir.getPath()));
            try {
                ModuleImpl[] modules = moduleLoader.loadModules(modulesDir, pm);
                getLogger().info(String.format("%d module(s) found.", modules.length));
                registerModules(modules);
            } catch (IOException e) {
                throw new CoreException(e);
            }
        }
    }

    private void loadModulesFromClasspath(ModuleLoader moduleLoader, ProgressMonitor pm) throws CoreException {
        try {
            getLogger().info("Searching for modules in classpath...");
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            ModuleImpl[] modules = moduleLoader.loadModules(contextClassLoader, pm);
            getLogger().info(String.format("%d module(s) found.", modules.length));
            registerModules(modules);
        } catch (IOException e) {
            throw new CoreException(e);
        }
    }

    private void registerModules(ModuleImpl[] modules) {
        for (ModuleImpl module : modules) {
            try {
                if (moduleRegistry.getModule(module.getLocation()) != null) {
                    getLogger().warning(
                            MessageFormat.format("Module [{0}-{1}@{2}] already registered.",
                                                 module.getSymbolicName(),
                                                 module.getVersion(),
                                                 module.getLocation()));
                } else {
                    long id = module.getSymbolicName().equals(SYSTEM_MODULE_NAME) ? 0L : newModuleId();
                    if (moduleRegistry.getModule(id) == null) {  // ceres-core is maybe already registered
                        registerModule(module, id);
                        getLogger().info(MessageFormat.format("Module [{0}-{1}] registered.",
                                                              module.getSymbolicName(),
                                                              module.getVersion()));
                    }
                }
            } catch (CoreException e) {
                logError(MessageFormat.format("Failed to register module [{0}-{1}@{2}].",
                                              module.getSymbolicName(),
                                              module.getVersion(),
                                              module.getLocation()), e);
            }
        }
    }

    private long newModuleId() {
        return ++lastModuleId;
    }

    private void initSystemModule() throws CoreException {
        ModuleImpl[] systemModules = moduleRegistry.getModules(SYSTEM_MODULE_NAME);
        if (systemModules.length > 0) {
            systemModule = systemModules[0];
        }
        if (systemModule == null) {
            URL location = getCodeSourceLocation();
            ModuleReader moduleReader = new ModuleReader(getLogger());
            systemModule = moduleReader.readFromLocation(location);
            registerModule(systemModule, 0L);
        }
    }

    private void registerModule(ModuleImpl module, long moduleId) throws CoreException {
        module.setRuntime(this);
        module.setModuleId(moduleId);
        moduleRegistry.registerModule(module);
    }

    private void resolveModules(ProgressMonitor pm) {

        ModuleImpl[] modules = moduleRegistry.getModules();
        resolvedModules = new ArrayList<>(modules.length);

        pm.beginTask("Resolving modules", modules.length + 1);

        try {
            for (ModuleImpl module : modules) {
                resolveModule(module);
                pm.worked(1);
            }

            Collections.sort(resolvedModules, (m1, m2) -> m2.getRefCount() - m1.getRefCount());
            pm.worked(1);

            logResolveSummary();
        } finally {
            pm.done();
        }
    }

    private void resolveModule(ModuleImpl module) {
        getLogger().info(MessageFormat.format("Resolving module [{0}-{1}].", module.getSymbolicName(), module.getVersion()));
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final ModuleResolver moduleResolver = new ModuleResolver(contextClassLoader, true);

        try {
            moduleResolver.resolve(module);
            systemModule.setRefCount(systemModule.getRefCount() + module.getRefCount());
            resolvedModules.add(module);
        } catch (ResolveException e) {
            logError(e.getMessage(), e);
        }

        List<ResolveException> resolveErrors = Arrays.asList(module.getResolveErrors());
        if (!resolveErrors.isEmpty()) {
            Collections.reverse(resolveErrors);
            for (ResolveException resolveError : resolveErrors) {
                getLogger().log(Level.SEVERE, resolveError.getMessage());
            }
        }

        List<ResolveException> resolveWarnings = Arrays.asList(module.getResolveWarnings());
        if (!resolveWarnings.isEmpty()) {
            Collections.reverse(resolveWarnings);
            for (ResolveException resolveWarning : resolveWarnings) {
                getLogger().log(Level.WARNING, resolveWarning.getMessage());
            }
        }
    }

    private void logResolveSummary() {
        for (ModuleImpl module : resolvedModules) {
            String msg = MessageFormat.format("Module [{0}-{1}] resolved, reference count is {2}.",
                                              module.getSymbolicName(),
                                              module.getVersion(),
                                              module.getRefCount());
            getLogger().fine(msg);
            // Note for all resolvedModules, always module.moduleDependencies != null,
            ModuleImpl[] moduleDependencies = module.getModuleDependencies();
            for (int i = 0; i < moduleDependencies.length; i++) {
                msg = MessageFormat.format("  moduleDependencies[{0}] = {1}", i, moduleDependencies[i].getSymbolicName());
                getLogger().fine(msg);
            }

            ModuleClassLoader classLoader = (ModuleClassLoader) module.getClassLoader();
            msg = MessageFormat.format("  classLoader = {0}", classLoader);
            getLogger().fine(msg);
            URL[] urls = classLoader.getURLs();
            for (int i = 0; i < urls.length; i++) {
                msg = MessageFormat.format("  classLoader.urls[{0}] = {1}", i, urls[i]);
                getLogger().fine(msg);
            }
            URL[] nativeUrls = classLoader.getNativeUrls();
            for (int i = 0; i < nativeUrls.length; i++) {
                msg = MessageFormat.format("  classLoader.nativeUrls[{0}] = {1}", i, nativeUrls[i]);
                getLogger().fine(msg);
            }
            ClassLoader[] delegates = classLoader.getDelegates();
            for (int i = 0; i < delegates.length; i++) {
                msg = MessageFormat.format("  classLoader.delegates[{0}] = {1}", i, delegates[i]);
                getLogger().fine(msg);
            }
        }
    }

    private void startModules(ProgressMonitor subProgressMonitor) {
        try {
            subProgressMonitor.beginTask("Starting modules", resolvedModules.size());
            for (ModuleImpl module : resolvedModules) {
                if (module.getState() == ModuleState.RESOLVED) {
                    try {
                        subProgressMonitor.setSubTaskName(module.getName());
                        module.start();
                        getLogger().info(MessageFormat.format("Module [{0}-{1}] started.",
                                                              module.getSymbolicName(),
                                                              module.getVersion()));
                    } catch (CoreException e) {
                        logError(MessageFormat.format("Failed to start module [{0}-{1}].",
                                                      module.getSymbolicName(),
                                                      module.getVersion()), e);
                    }
                }
                subProgressMonitor.worked(1);
            }
        } finally {
            subProgressMonitor.done();
        }
    }

    private void stopModules() {
        for (int i = resolvedModules.size() - 1; i >= 0; i--) {
            ModuleImpl module = resolvedModules.get(i);
            if (module.getState() == ModuleState.ACTIVE) {
                try {
                    module.stop();
                    getLogger().info(MessageFormat.format("Module [{0}-{1}] stopped.",
                                                          module.getSymbolicName(),
                                                          module.getVersion()));
                } catch (CoreException e) {
                    logError(MessageFormat.format("Failed to stop module [{0}-{1}].",
                                                  module.getSymbolicName(),
                                                  module.getVersion()), e);
                }
            }
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    RuntimeImpl.this.stop();
                } catch (CoreException e) {
                    logError("Failed to shutdown runtime.", e);
                }
            }
        });
        getLogger().info("Shutdown hook registered.");
    }

    private void runApplication(ProgressMonitor pm) throws CoreException {
        String applicationId = config.getApplicationId();
        if (applicationId == null) {
            return;
        }
        RuntimeRunnable application = getRuntimeActivator().getApplication(applicationId);
        if (application == null) {
            throw new CoreException(MessageFormat.format("Application [{0}] not found", applicationId));
        }
        try {
            getLogger().info(MessageFormat.format("Invoking application [{0}].", applicationId));
            // note: executing foreign code here!
            application.run(commandLineArgs, pm);
            getLogger().info(MessageFormat.format("Application [{0}] invoked.", applicationId));
        } catch (Throwable t) {
            throw new CoreException(MessageFormat.format("Failed to invoke application [{0}]", applicationId), t);
        }
    }

    private RuntimeActivator getRuntimeActivator() {
        return ((RuntimeActivator) systemModule.getActivator());
    }

    private URL getCodeSourceLocation() throws CoreException {
        CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new CoreException("No code source available for system module");
        }
        return codeSource.getLocation();
    }

    private void logError(String msg, Throwable e) {
        getLogger().log(Level.SEVERE, msg, e);
    }
}
