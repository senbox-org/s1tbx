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
import com.bc.ceres.core.runtime.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * A strategy which resolves module dependencies.
 */
public class ModuleResolver {

    private ClassLoader moduleParentClassLoader;
    private boolean resolvingLibs;
    private Stack<String> moduleStack;

    /**
     * Constructs a new module resolver.
     *
     * @param moduleParentClassLoader the parent class loader for the module
     * @param resolvingLibs           if true, libs are resolved
     */
    public ModuleResolver(ClassLoader moduleParentClassLoader, boolean resolvingLibs) {
        Assert.notNull(moduleParentClassLoader, "moduleParentClassLoader");
        this.moduleParentClassLoader = moduleParentClassLoader;
        this.resolvingLibs = resolvingLibs;
        this.moduleStack = new Stack<String>();
    }

    public void resolve(ModuleImpl module) throws ResolveException {
        Assert.notNull(module, "module");
        ModuleState previousState = module.getState();
        initModuleDependencies(module);
        if (module.getState() == ModuleState.RESOLVED) {
            initModuleClassLoader(module);
            initRefCount(module);
        }
        if (module.hasResolveErrors()) {
            module.setState(previousState);
            String msg = String.format("Failed to resolve module [%s].", module.getSymbolicName());
            throw new ResolveException(msg);
        }
    }

    static class DependencyItem {

        ModuleImpl module;
        boolean optional;

        public DependencyItem(ModuleImpl module, boolean optional) {
            this.module = module;
            this.optional = optional;
        }
    }

    private void initModuleDependencies(ModuleImpl module) {
        if (module.hasResolveErrors()) {
            return;
        }

        if (module.getState() == ModuleState.INSTALLED) {

            if (module.getModuleDependencies() == null) {
                ModuleImpl[] resolvedModules = resolveModuleDependencies(module);
                module.setModuleDependencies(resolvedModules);
            }

            if (module.getDeclaredLibs() == null) {
                String[] declaredLibs = findDeclaredLibs(module);
                module.setDeclaredLibs(declaredLibs);
            }

            if (module.getLibDependencies() == null) {
                URL[] libDependencies = findLibDependencies(module);
                module.setLibDependencies(libDependencies);
            }

            if (!module.hasResolveErrors()) {
                module.setState(ModuleState.RESOLVED);
            }
        }
    }

    private ModuleImpl[] resolveModuleDependencies(ModuleImpl module) {

        String moduleKey = module.getSymbolicName() + ":" + module.getVersion();
        if (moduleStack.contains(moduleKey)) {
            String message = createCyclicDependecyExceptionMessage(module);
            module.addResolveError(new ResolveException(message));
            return new ModuleImpl[0];
        }

        try {
            moduleStack.push(moduleKey);
            return resolveModuleDependenciesImpl(module);
        } finally {
            moduleStack.pop();
        }
    }

    private String createCyclicDependecyExceptionMessage(ModuleImpl module) {
        StringBuilder trace = new StringBuilder();
        for (String s : moduleStack) {
            trace.append('[').append(s).append(']');
        }
        return MessageFormat.format("Cyclic dependencies detected for module [{0}], trace: {1}",
                                    module.getSymbolicName(), trace);
    }

    private ModuleImpl[] resolveModuleDependenciesImpl(ModuleImpl module) {
        DependencyItem[] moduleDependencies = findModuleDependencies(module);
        List<ModuleImpl> resolvedModules = new ArrayList<ModuleImpl>(moduleDependencies.length);
        for (DependencyItem dependencyItem : moduleDependencies) {
            initModuleDependencies(dependencyItem.module);
            if (dependencyItem.module.getState() == ModuleState.RESOLVED) {
                resolvedModules.add(dependencyItem.module);
            }
            ResolveException[] resolveErrors = dependencyItem.module.getResolveErrors();
            if (resolveErrors.length > 0) {
                for (ResolveException resolveError : resolveErrors) {
                    if (dependencyItem.optional) {
                        module.addResolveWarning(resolveError);
                    } else {
                        module.addResolveError(resolveError);
                    }
                }
            }
            ResolveException[] resolveWarnings = dependencyItem.module.getResolveWarnings();
            if (resolveWarnings.length > 0) {
                for (ResolveException resolveWarning : resolveWarnings) {
                    module.addResolveWarning(resolveWarning);
                }
            }
        }
        return resolvedModules.toArray(new ModuleImpl[resolvedModules.size()]);
    }

    private void initModuleClassLoader(ModuleImpl module) {
        if (module.getClassLoader() == null) {

            URL[] nativeLibs = getNativeLibs(module);
            URL[] dependencyLibs = module.getLibDependencies();
            if (dependencyLibs == null) {
                dependencyLibs = new URL[0];
            }
            ClassLoader[] dependencyClassLoaders = getDependencyClassLoaders(module);

            module.setClassLoader(new ModuleClassLoader(dependencyClassLoaders, dependencyLibs,
                                                        nativeLibs, moduleParentClassLoader));
        }
    }

    private ClassLoader[] getDependencyClassLoaders(ModuleImpl module) {
        List<ClassLoader> dependencyCl = new ArrayList<ClassLoader>();
        for (ModuleImpl moduleDependency : module.getModuleDependencies()) {
            if (moduleDependency.getState() == ModuleState.RESOLVED) {
                if (moduleDependency.getClassLoader() == null) {
                    // Enter recursion
                    initModuleClassLoader(moduleDependency);
                }
                dependencyCl.add(moduleDependency.getClassLoader());
            }
        }
        return dependencyCl.toArray(new ClassLoader[dependencyCl.size()]);
    }

    private static URL[] getNativeLibs(ModuleImpl module) {
        List<URL> libPaths = new ArrayList<URL>();
        if (module.isNative()) {
            File moduleDir = UrlHelper.urlToFile(module.getLocation());
            if (moduleDir.isDirectory()) {
                String[] impliciteNativeLibs = module.getImpliciteNativeLibs();
                for (String libPath : impliciteNativeLibs) {
                    File libFile = new File(moduleDir, libPath);
                    if (libFile.isFile() && libFile.canRead()) {
                        libPaths.add(UrlHelper.fileToUrl(libFile));
                    } else {
                        String msg = String.format("Native library [%s] found in module [%s] is not accessible.",
                                                   libFile, module.getSymbolicName());
                        module.addResolveWarning(new ResolveException(msg));
                    }
                }
            }
        }

        return libPaths.toArray(new URL[libPaths.size()]);
    }

    private static void initRefCount(ModuleImpl module) {
        module.incrementRefCount();
        if (module.getModuleDependencies() != null) {
            ModuleImpl[] moduleDependencies = module.getModuleDependencies();
            for (ModuleImpl moduleDependency : moduleDependencies) {
                // enter recursion
                initRefCount(moduleDependency);
            }
        }
    }

    private URL[] findLibDependencies(ModuleImpl module) {
        if (module.getLocation() == null) {
            throw new IllegalStateException("module.getLocation() == null");
        }
        if (module.getModuleDependencies() == null) {
            throw new IllegalStateException("module.getModuleDependencies() == null");
        }
        if (module.getDeclaredLibs() == null) {
            throw new IllegalStateException("module.getDeclaredLibs() == null");
        }
        if (module.getImpliciteLibs() == null) {
            throw new IllegalStateException("module.getImpliciteLibs() == null");
        }

        File moduleFile = UrlHelper.urlToFile(module.getLocation());
        if (moduleFile == null) {
            return new URL[0];
        }

        List<URL> libDependencies = new ArrayList<URL>(16);

        // add this module to the classpath
        collectLibDependency(module, moduleFile, libDependencies);

        // add all declared libs to the classpath
        resolveLibs(module,
                    moduleFile, resolvingLibs, libDependencies);

        // add all implicite libs to the classpath
        for (String impliciteLib : module.getImpliciteLibs()) {
            collectLibDependency(module, new File(moduleFile, impliciteLib), libDependencies);
        }

        return libDependencies.toArray(new URL[0]);
    }

    private static void resolveLibs(ModuleImpl module, File moduleFile,
                                    boolean resolvingLibs, List<URL> libDependencies) {
        Dependency[] declaredDependencies = module.getDeclaredDependencies();
        for (Dependency dependency : declaredDependencies) {
            if (dependency.getLibName() != null) {
                boolean libResolved = false;
                // look in this modules location
                File file = resolveFile(moduleFile, dependency.getLibName(), resolvingLibs);
                if (file != null) {
                    collectLibDependency(module, file, libDependencies);
                    libResolved = true;
                } else {
                    for (ModuleImpl moduleDependency : module.getModuleDependencies()) {
                        // look in the modules dependencies' location
                        File moduleDependencyFile = UrlHelper.urlToFile(moduleDependency.getLocation());
                        if (moduleDependencyFile != null) {
                            File file2 = resolveFile(moduleDependencyFile, dependency.getLibName(), resolvingLibs);
                            if (file2 != null) {
                                collectLibDependency(module, file2, libDependencies);
                                libResolved = true;
                                break;
                            }
                        }
                    }
                }
                // library is not resolved and we must resolve dependecies
                if (!libResolved && resolvingLibs) {
                    if (!dependency.isOptional()) {
                        String msg = String.format("Mandatory library [%s] declared by module [%s] not found.",
                                                   dependency.getLibName(),
                                                   module.getSymbolicName());
                        module.addResolveError(new ResolveException(msg));
                    }
                }
            }
        }
    }

    private static File resolveFile(File parent, String libPath, boolean checkExists) {
        if (parent.isDirectory()) {
            File file = new File(parent, libPath);
            if (!checkExists || file.exists()) {
                return file;
            }
        }
        return null;
    }

    private static DependencyItem[] findModuleDependencies(ModuleImpl module) {
        List<DependencyItem> list = new ArrayList<DependencyItem>(16);
        collectDeclaredModuleDependencies(module, list);
        collectImpliciteModuleDependencies(module, list);
        return list.toArray(new DependencyItem[0]);
    }


    private static void collectDeclaredModuleDependencies(ModuleImpl module, List<DependencyItem> list) {
        Dependency[] dependencies = module.getDeclaredDependencies();
        for (Dependency dependency : dependencies) {
            if (dependency.getModuleSymbolicName() != null) {
                ModuleImpl[] dependencyModules = module.getRegistry().getModules(dependency.getModuleSymbolicName());
                if (dependencyModules.length > 0) {
                    if (dependency.getVersion() != null) {
                        Version requiredVersion = Version.parseVersion(dependency.getVersion());
                        ModuleImpl dependencyModule = findBestMatchingModuleVersion(requiredVersion, dependencyModules);
                        if (dependencyModule != null && requiredVersion.compareTo(dependencyModule.getVersion()) <= 0) {
                            collectDependencyModule(module, dependencyModule, dependency.isOptional(), list);
                        } else if (!dependency.isOptional()) {
                            String msg = String.format(
                                    "Mandatory dependency [%s:%s] declared by module [%s] not found.",
                                    dependency.getModuleSymbolicName(),
                                    dependency.getVersion(),
                                    module.getSymbolicName());
                            module.addResolveError(new ResolveException(msg));
                        }
                    } else {
                        if (dependencyModules.length > 0) {
                            ModuleImpl dependencyModule = findLatestModuleVersion(dependencyModules);
                            collectDependencyModule(module, dependencyModule, dependency.isOptional(), list);
                        }
                    }
                } else if (!dependency.isOptional()) {
                    String msg = String.format("Mandatory dependency [%s] declared by module [%s] not found.",
                                               dependency.getModuleSymbolicName(),
                                               module.getSymbolicName());
                    module.addResolveError(new ResolveException(msg));
                }
            }
        }
    }

    private static void collectImpliciteModuleDependencies(ModuleImpl module, List<DependencyItem> list) {
        Extension[] extensions = module.getExtensions();
        for (Extension extension : extensions) {
            ExtensionPoint extensionPoint = extension.getExtensionPoint();
            if (extensionPoint != null) {
                collectDependencyModule(module, (ModuleImpl) extensionPoint.getDeclaringModule(), true, list);
            } else {
                String msg = String.format(
                        "Extension point [%s] used by module [%s] not found. Extension will be ignored.",
                        extension.getPoint(),
                        module.getSymbolicName());
                module.addResolveWarning(new ResolveException(msg));
            }
        }
    }

    private static ModuleImpl findLatestModuleVersion(ModuleImpl[] modules) {
        ModuleImpl latestModule = modules[0];
        Version latestVersion = latestModule.getVersion();
        for (int i = 1; i < modules.length; i++) {
            ModuleImpl module = modules[i];
            Version version = module.getVersion();
            if (version.compareTo(latestVersion) > 0) {
                latestModule = module;
                latestVersion = version;
            }
        }
        return latestModule;
    }

    private static Version[] getVersions(Module[] modules) {
        Version[] versions = new Version[modules.length];
        for (int i = 0; i < modules.length; i++) {
            versions[i] = modules[i].getVersion();
        }
        return versions;
    }

    private static ModuleImpl findBestMatchingModuleVersion(Version requiredVersion, ModuleImpl[] modules) {
        Version[] versions = getVersions(modules);

        for (int i = 0; i < modules.length; i++) {
            ModuleImpl module = modules[i];
            if (versions[i].compareTo(requiredVersion) == 0) {
                return module;
            }
        }

        ModuleImpl bestModule = findLatestModuleVersion(modules);
        Version bestVersion = bestModule.getVersion();
        for (int i = 0; i < modules.length; i++) {
            ModuleImpl module = modules[i];
            Version version = versions[i];
            if (version.compareTo(requiredVersion) == 0) {
                bestModule = module;
                break;
            } else if (version.compareTo(requiredVersion) > 0
                       && version.compareTo(bestVersion) < 0) {
                bestModule = module;
                bestVersion = version;
            }
        }

        return bestModule;
    }

    private static String[] findDeclaredLibs(ModuleImpl module) {
        Dependency[] dependencies = module.getDeclaredDependencies();
        ArrayList<String> libNames = new ArrayList<String>(dependencies.length);
        for (Dependency dependency : dependencies) {
            // if dependency.getLibName() is not null we have a declared JAR dependency,
            // otherwise it is expected that dependency.getModuleId() will return a non-null value
            // which means we have a declared module dependency.
            if (dependency.getLibName() != null) {
                if (!libNames.contains(dependency.getLibName())) {
                    libNames.add(dependency.getLibName());
                }
            }
        }
        return libNames.toArray(new String[0]);
    }

    private static void collectDependencyModule(ModuleImpl module, ModuleImpl dependencyModule, boolean optional,
                                                List<DependencyItem> dependencyItems) {
        if (dependencyModule == module) {
            return;
        }
        for (DependencyItem dependencyItem : dependencyItems) {
            if (dependencyModule == dependencyItem.module) {
                return;
            }
        }
        dependencyItems.add(new DependencyItem(dependencyModule, optional));
    }

    private static void collectLibDependency(ModuleImpl module, File lib, List<URL> list) {
        try {
            URL url = convertToURL(lib);
            if (!list.contains(url)) {
                list.add(url);
            }
        } catch (MalformedURLException e) {
            String msg = String.format("Library file path [%s] used by module [%s] cannot be converted to an URL.",
                                       lib.getPath(),
                                       module.getSymbolicName());
            module.addResolveError(new ResolveException(msg, e));
        }
    }

    private static URL convertToURL(File lib) throws MalformedURLException {
        return lib.toURI().toURL();
    }
}
