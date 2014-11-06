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
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.Dependency;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ExtensionPoint;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.Version;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * The {@link Module} default implementation.
 * Instances of this class can only be created via the {@link ModuleManifestParser}.
 */
public class ModuleImpl implements Module {

    public static final ModuleImpl[] EMPTY_ARRAY = new ModuleImpl[0];

    // The following are all initialised by the ModuleManifestParser
    private String manifestVersion;
    private String symbolicName;
    private String name;
    private Version version;
    private String description;
    private String packaging;
    private String activatorClassName;
    private String categoriesString; // IDE warning "private field never assigned" is ok
    private String changelog; // IDE warning "private field never used" is ok
    private String copyright; // IDE warning "private field never used" is ok
    private String vendor; // IDE warning "private field never used" is ok
    private String contactAddress; // IDE warning "private field never used" is ok
    private String funding; // IDE warning "private field never used" is ok
    private String url; // IDE warning "private field never used" is ok
    private boolean usingJni; // IDE warning "private field never assigned" is ok
    private String aboutUrl;
    private String licenseUrl;
    private ArrayList<DependencyImpl> declaredDependencies; // IDE warning "private field never assigned" is ok
    private ArrayList<ExtensionPointImpl> extensionPoints; // IDE warning "private field never assigned" is ok
    private ArrayList<ExtensionImpl> extensions;  // IDE warning "private field never assigned" is ok

    // The following are initialised by the framework
    private transient String[] categories;
    private transient RuntimeImpl runtime; // initialised by RuntimeImpl
    private transient long moduleId; // initialised by RuntimeImpl
    private transient ModuleState state; // set by various components
    private transient ModuleRegistry registry; // initialised by ModuleRegistry of RuntimeImpl

    // The following are initialised by the ModuleReader
    private transient URL location;
    private transient String[] impliciteLibs;
    private transient String[] impliciteNativeLibs;
    private transient long contentLength;
    private transient long lastModified;

    // The following are initialised by the ModuleResolver
    private transient String[] declaredLibs;
    private transient URL[] libDependencies;
    private transient ModuleImpl[] moduleDependencies;
    private transient ClassLoader classLoader;
    private List<ResolveException> resolveWarnings;
    private List<ResolveException> resolveErrors;
    private transient int refCount;

    private transient Activator activator; // initialised by ModuleStarter
    private transient ModuleContext context;  // initialised by ModuleStarter

    // avoid direct instantiation, modules can only be instantiated by the ModuleManfestParser

    private ModuleImpl() {
    }


    public String getManifestVersion() {
        return manifestVersion;
    }

    public long getModuleId() {
        return moduleId;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public Version getVersion() {
        return version;
    }

    public ModuleState getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public String getVendor() {
        return vendor;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getCopyright() {
        return copyright;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public String getFunding() {
        return funding == null ? "" : funding;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public String[] getCategories() {
        return categories;
    }

    public String getActivatorClassName() {
        return activatorClassName;
    }

    public String getPackaging() {
        return packaging;
    }

    public URL getLocation() {
        return location;
    }

    public boolean isNative() {
        return usingJni;
    }

    public String getAboutUrl() {
        return aboutUrl;
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        checkClassLoader();
        return getClassLoader().loadClass(name);
    }

    public URL getResource(String name) {
        checkClassLoader();
        return getClassLoader().getResource(name);
    }

    public InputStream getResourceAsStream(String name) {
        checkClassLoader();
        return getClassLoader().getResourceAsStream(name);
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        checkClassLoader();
        return getClassLoader().getResources(name);
    }

// Introduce not before usage of JRE 1.6 is agreed.
/*
    public <S> ServiceLoader<S> getServices(Class<S> service) {
        checkClassLoader();
        return ServiceLoader.load(service, getClassLoader());
    }
*/

    public Dependency[] getDeclaredDependencies() {
        return declaredDependencies != null ? declaredDependencies.toArray(
                new DependencyImpl[declaredDependencies.size()]) : DependencyImpl.EMPTY_ARRAY;
    }

    public ExtensionPoint[] getExtensionPoints() {
        return extensionPoints != null ? extensionPoints.toArray(
                new ExtensionPointImpl[extensionPoints.size()]) : ExtensionPointImpl.EMPTY_ARRAY;
    }

    public Extension[] getExtensions() {
        return extensions != null ? extensions.toArray(
                new ExtensionImpl[extensions.size()]) : ExtensionImpl.EMPTY_ARRAY;
    }

    public ExtensionPoint getExtensionPoint(String extensionPointId) {
        if (extensionPoints == null) {
            return null;
        }
        for (ExtensionPointImpl extensionPoint : extensionPoints) {
            if (extensionPoint.getId().equals(extensionPointId)) {
                return extensionPoint;
            }
        }
        return registry.getExtensionPoint(extensionPointId);
    }

    public Extension getExtension(String extensionId) {
        if (extensions == null) {
            return null;
        }
        for (Extension extension : extensions) {
            if (extensionId.equals(extension.getId())) {
                return extension;
            }
        }
        return null;
    }


    @Override
    public int hashCode() {
        return symbolicName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[moduleId=" + moduleId + ", symbolicName=" + symbolicName + ", version=" + version + "]";
    }

    public void uninstall(ProgressMonitor pm) throws CoreException {
        ModuleUninstaller moduleUninstaller = new ModuleUninstaller(runtime.getLogger());
        try {
            moduleUninstaller.uninstallModule(this);
        } catch (IOException e) {
            throw new CoreException(e);
        }
    }

    // end of public API
    /////////////////////////////////////////////////////////////////////////

    String getCategoriesString() {
        return categoriesString;
    }

    public void setManifestVersion(String manifestVersion) {
        this.manifestVersion = manifestVersion;
    }

    void setCategories(String[] categories) {
        this.categories = categories;
    }

    void setModuleId(long moduleId) {
        checkRegistered();
        this.moduleId = moduleId;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    void setName(String name) {
        this.name = name;
    }

    /*internal*/

    public void setVersion(Version version) {
        this.version = version;
    }

    void setDescription(String description) {
        this.description = description;
    }

    public void setAboutUrl(String aboutUrl) {
        this.aboutUrl = aboutUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    void setActivatorClassName(String activatorClassName) {
        this.activatorClassName = activatorClassName;
    }

    void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    void setRefCount(int refCount) {
        this.refCount = refCount;
    }


    void initDeclaredComponents() {
        if (declaredDependencies != null) {
            for (DependencyImpl dependency : declaredDependencies) {
                dependency.setDeclaringModule(this);
            }
        }
        if (extensionPoints != null) {
            for (ExtensionPointImpl extensionPoint : extensionPoints) {
                extensionPoint.setDeclaringModule(this);
            }
        }
        if (extensions != null) {
            for (ExtensionImpl extension : extensions) {
                extension.setDeclaringModule(this);
            }
        }
    }

    ModuleRegistry getRegistry() {
        return registry;
    }

    void setRegistry(ModuleRegistry registry) {
        checkRegistered();
        this.registry = registry;
    }

    public void setLocation(URL location) {
        checkRegistered();
        this.location = location;
    }

    String[] getImpliciteLibs() {
        return impliciteLibs;
    }

    void setImpliciteLibs(String[] impliciteLibs) {
        this.impliciteLibs = impliciteLibs;
    }

    String[] getImpliciteNativeLibs() {
        return impliciteNativeLibs;
    }

    void setImpliciteNativeLibs(String[] impliciteNativeLibs) {
        this.impliciteNativeLibs = impliciteNativeLibs;
    }

    String[] getDeclaredLibs() {
        return declaredLibs;
    }

    void setDeclaredLibs(String[] declaredLibs) {
        this.declaredLibs = declaredLibs;
    }

    URL[] getLibDependencies() {
        return libDependencies;
    }

    void setLibDependencies(URL[] libDependencies) {
        this.libDependencies = libDependencies;
    }

    ModuleImpl[] getModuleDependencies() {
        return moduleDependencies;
    }

    void setModuleDependencies(ModuleImpl[] moduleDependencies) {
        this.moduleDependencies = moduleDependencies;
    }

    /*internal*/

    public void setState(ModuleState state) {
        this.state = state;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    Activator getActivator() {
        return activator;
    }

    void setActivator(Activator activator) {
        this.activator = activator;
    }

    ModuleContext getContext() {
        return context;
    }

    void setContext(ModuleContext context) {
        this.context = context;
    }

    int getRefCount() {
        return refCount;
    }

    void incrementRefCount() {
        refCount++;
    }

    RuntimeImpl getRuntime() {
        return runtime;
    }

    void setRuntime(RuntimeImpl runtime) {
        this.runtime = runtime;
    }

    void start() throws CoreException {
        new ModuleStarter(this).run();
    }

    void stop() throws CoreException {
        new ModuleStopper(this).run();
    }

    private void checkClassLoader() {
        if (classLoader == null) {
            throw new IllegalStateException("classLoader == null");
        }
    }

    private void checkRegistered() {
        if (registry != null) {
            throw new IllegalStateException("illegal operation, module already registered");
        }
    }

    boolean hasResolveErrors() {
        return hasResolveExceptions(resolveErrors);
    }

    ResolveException[] getResolveErrors() {
        return getResolveExceptions(resolveErrors);
    }

    void addResolveError(ResolveException resolveException) {
        resolveErrors = addResolveException(resolveException, resolveErrors);
    }

    ResolveException[] getResolveWarnings() {
        return getResolveExceptions(resolveWarnings);
    }

    void addResolveWarning(ResolveException resolveException) {
        resolveWarnings = addResolveException(resolveException, resolveWarnings);
    }

    private boolean hasResolveExceptions(List<ResolveException> resolveExceptions) {
        return resolveExceptions != null && !resolveExceptions.isEmpty();
    }

    private ResolveException[] getResolveExceptions(List<ResolveException> resolveExceptionList) {
        if (resolveExceptionList == null) {
            return new ResolveException[0];
        }
        return resolveExceptionList.toArray(new ResolveException[resolveExceptionList.size()]);
    }

    private List<ResolveException> addResolveException(ResolveException resolveException,
                                                       List<ResolveException> resolveExceptionList) {
        if (resolveExceptionList == null) {
            resolveExceptionList = new ArrayList<ResolveException>(3);
        }
        int n = resolveExceptionList.size();
        if (n > 0) {
            String message = resolveException.getMessage();
            String lastMessage = resolveExceptionList.get(n - 1).getMessage();
            if (lastMessage != null && lastMessage.equals(message)) {
                return resolveExceptionList;
            }
        }
        resolveExceptionList.add(resolveException);
        return resolveExceptionList;
    }

    @Override
    public int compareTo(Module o) {
        return this.getName().compareTo(o.getName());
    }
}
