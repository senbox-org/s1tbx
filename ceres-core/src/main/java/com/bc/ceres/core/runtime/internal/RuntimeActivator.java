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
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import com.bc.ceres.core.SingleTypeExtensionFactory;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ExtensionPoint;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.RuntimeRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

public class RuntimeActivator implements Activator {

    private static RuntimeActivator instance;

    private Map<String, RuntimeRunnable> applications;
    private List<ServiceRegistration> serviceRegistrations;
    private ModuleContext moduleContext;
    private ClassLoader resourcesClassLoader;

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

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        this.moduleContext = moduleContext;
        initApplications();
        initServiceProviders();
        initAdapters();
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
        try {
            disposeServiceProviders();
            disposeApplications();
        } finally {
            this.moduleContext = null;
        }
    }

    public ClassLoader getResourcesClassLoader() {
        if (resourcesClassLoader == null) {
            synchronized (this) {
                if (resourcesClassLoader == null) {
                    resourcesClassLoader = createResourcesClassLoader();
                }
            }
        }
        return resourcesClassLoader;
    }


    private void initServiceProviders() {
        ClassLoader providerLoader = getResourcesClassLoader();
        serviceRegistrations = new ArrayList<>(32);
        ExtensionPoint extensionPoint = moduleContext.getModule().getExtensionPoint("serviceProviders");
        Extension[] extensions = extensionPoint.getExtensions();
        for (Extension extension : extensions) {
            ConfigurationElement[] children = extension.getConfigurationElement().getChildren("serviceProvider");
            for (ConfigurationElement child : children) {
                String providerClassName = child.getValue();
                Module declaringModule = extension.getDeclaringModule();
                if (declaringModule.getState().is(ModuleState.RESOLVED)) {
                    Class<?> providerClass = getProviderClass(declaringModule, providerClassName);
                    if (providerClass != null) {
                        try {
                            Set<ServiceRegistration> serviceRegistrationsForClass = getServiceRegistrations(providerClass, providerLoader);
                            for (ServiceRegistration serviceRegistration : serviceRegistrationsForClass) {
                                String[] providerImplClassNames = getProviderImplClassNames(serviceRegistration);
                                if (providerImplClassNames != null) {
                                    for (String providerImplClassName : providerImplClassNames) {
                                        Class<?> providerImplClass = getProviderImplClass(serviceRegistration, providerImplClassName);
                                        if (providerImplClass != null) {
                                            registerProviderImpl(serviceRegistration, providerImplClass);
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            moduleContext.getLogger().log(Level.SEVERE,
                                                          String.format("Failed to load service provider [%s]",
                                                                        providerClassName), e);
                        }
                    }
                }
            }
        }
    }

    private ClassLoader createResourcesClassLoader() {
        ArrayList<URL> urlArrayList = new ArrayList<>();
        for (Module module : moduleContext.getModules()) {
            if (module.getState().is(ModuleState.RESOLVED)) {
                URL location = module.getLocation();
                urlArrayList.add(location);
            }
        }
        return new URLClassLoader(urlArrayList.toArray(new URL[urlArrayList.size()]), new NullClassLoader());
    }

    private void registerProviderImpl(ServiceRegistration serviceRegistration, Class<?> providerImplClass) {
        Class<?> providerClass = serviceRegistration.serviceRegistry.getServiceType();
        if (providerClass.isAssignableFrom(providerImplClass)) {
            Object providerImpl = getProviderImpl(providerImplClass);
            if (providerImpl != null) {
                serviceRegistration.serviceRegistry.addService(providerImpl);
                serviceRegistration.providerImpl = providerImpl;
                moduleContext.getLogger().info(String.format("Module [%s]: Service [%s] registered",
                                                             serviceRegistration.module.getSymbolicName(),
                                                             serviceRegistration.providerImpl.getClass()));
                serviceRegistrations.add(serviceRegistration);
            }
        } else {
            moduleContext.getLogger().severe(String.format("Service [%s] is not of type [%s]",
                                                           providerImplClass.toString(),
                                                           providerClass.toString()));
        }
    }

    private Object getProviderImpl(Class<?> providerImplClass) {
        Object providerImpl = null;
        try {
            providerImpl = providerImplClass.newInstance();
        } catch (Throwable t) {
            moduleContext.getLogger().log(Level.SEVERE,
                                          String.format("Failed to instantiate service of type [%s]",
                                                        providerImplClass.toString()), t);
        }
        return providerImpl;
    }

    private Class<?> getProviderImplClass(ServiceRegistration serviceRegistration, String providerImplClassName) {
        Class<?> providerImplClass = null;
        try {
            providerImplClass = serviceRegistration.module.loadClass(providerImplClassName);
        } catch (Throwable t) {
            moduleContext.getLogger().log(Level.SEVERE,
                                          String.format("Failed to load service type [%s]",
                                                        providerImplClassName), t);
        }
        return providerImplClass;
    }

    private String[] getProviderImplClassNames(ServiceRegistration serviceRegistration) {
        String[] providerImplClassNames = null;
        try {
            providerImplClassNames = parseSpiConfiguration(serviceRegistration.url);
        } catch (IOException e) {
            moduleContext.getLogger().log(Level.SEVERE,
                                          String.format(
                                                  "Failed to load configuration [%s] from module [%s].",
                                                  serviceRegistration.url,
                                                  serviceRegistration.module.getName()), e);
        }
        return providerImplClassNames;
    }

    private Class<?> getProviderClass(Module declaringModule, String providerClassName) {
        Class<?> providerClass = null;
        try {
            providerClass = declaringModule.loadClass(providerClassName);
        } catch (Throwable t) {
            moduleContext.getLogger().log(Level.SEVERE,
                                          String.format("Failed to load service provider [%s].",
                                                        providerClassName), t);
        }
        return providerClass;
    }

    private Set<ServiceRegistration> getServiceRegistrations(Class<?> providerClass, ClassLoader providerLoader) throws IOException {
        ServiceRegistry serviceRegistry = ServiceRegistryManager.getInstance().getServiceRegistry(providerClass);
        HashSet<ServiceRegistration> serviceRegistrationsForClass = new HashSet<ServiceRegistration>(10);
        String resourcePath = "META-INF/services/" + providerClass.getName();

        Enumeration<URL> resources = providerLoader.getResources(resourcePath);
        if (resources != null) {
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                Module module = getModule(url);
                if (module != null) {
                    ServiceRegistration serviceRegistration = new ServiceRegistration(url, module, serviceRegistry);
                    if (!serviceRegistrationsForClass.contains(serviceRegistration)) {
                        serviceRegistrationsForClass.add(serviceRegistration);
                    } else {
                        moduleContext.getLogger().warning(String.format("Service already registered: [%s].", serviceRegistration));
                    }
                } else {
                    moduleContext.getLogger().warning("Module not found for service provider URL " + url);
                }
            }
        }

        return serviceRegistrationsForClass;
    }

    private Module getModule(URL url) {
        String urlString = url.toExternalForm();
        Module module = getModule(urlString);
        if (module == null && urlString.startsWith("jar:")) {
            urlString = urlString.substring(4);
            module = getModule(urlString);
        }
        return module;
    }

    private Module getModule(String urlAsString) {
        for (Module module : moduleContext.getModules()) {
            if (urlAsString.startsWith(module.getLocation().toExternalForm())) {
                return module;
            }
        }
        return null;
    }

    private static class ServiceRegistration {

        final URL url;
        final Module module;
        final ServiceRegistry serviceRegistry;
        Object providerImpl;

        public ServiceRegistration(URL url, Module module, ServiceRegistry serviceRegistry) {
            this.url = url;
            this.module = module;
            this.serviceRegistry = serviceRegistry;
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return url.equals(((ServiceRegistration) obj).url);
        }

        @Override
        public String toString() {
            return url.toString();
        }
    }

    private void disposeServiceProviders() {
        for (ServiceRegistration serviceRegistration : serviceRegistrations) {
            ServiceRegistry serviceRegistry = serviceRegistration.serviceRegistry;
            Object providerImpl = serviceRegistration.providerImpl;
            serviceRegistry.removeService(providerImpl);
            moduleContext.getLogger().info(String.format("Module [%s]: Service [%s] unregistered",
                                                         serviceRegistration.module.getSymbolicName(),
                                                         serviceRegistration.providerImpl.getClass()));
        }
        serviceRegistrations.clear();
        resourcesClassLoader = null;
    }


    private void initApplications() {
        applications = new HashMap<>(3);
        ExtensionPoint extensionPoint = moduleContext.getModule().getExtensionPoint("applications");
        Extension[] extensions = extensionPoint.getExtensions();
        for (int i = 0; i < extensions.length; i++) {
            Extension extension = extensions[i];

            ConfigurationElement[] children = extension.getConfigurationElement().getChildren("application");
            for (ConfigurationElement child : children) {
                String appId = child.getAttribute("id");
                if (isNullOrEmpty(appId)) {
                    moduleContext.getLogger().severe(
                            "Missing identifier for extension " + i + " of extension point [applications].");
                    continue;
                } else if (applications.containsKey(appId)) {
                    moduleContext.getLogger().warning(
                            "Identifier [" + appId + "] is already in use within extension point [applications].");
                }
                RuntimeRunnable application = null;
                try {
                    // Run client code
                    application = child.createExecutableExtension(RuntimeRunnable.class);
                } catch (Throwable e) {
                    Module declaringModule = extension.getDeclaringModule();
                    String msg = String.format("Failed to register application [%s] (declared by module [%s]).",
                                               appId, declaringModule.getSymbolicName());
                    moduleContext.getLogger().log(Level.SEVERE, msg, e);
                }
                if (application != null) {
                    applications.put(appId, application);
                    Module declaringModule = extension.getDeclaringModule();
                    String msg = String.format("Module [%s]: Application [%s] registered using instance of [%s].",
                                               declaringModule.getSymbolicName(), appId, application.getClass());
                    moduleContext.getLogger().info(msg);
                }
            }
        }
    }

    private void disposeApplications() {
        applications.clear();
        applications = null;
    }

    private void initAdapters() {
        ExtensionPoint extensionPoint = moduleContext.getModule().getExtensionPoint("adapters");
        Extension[] extensions = extensionPoint.getExtensions();
        for (Extension extension : extensions) {
            ConfigurationElement[] children = extension.getConfigurationElement().getChildren("adapter");
            for (ConfigurationElement child : children) {
                try {
                    registerAdapter(extension, child);
                } catch (Exception e) {
                    moduleContext.getLogger().log(Level.SEVERE,
                                                  String.format("Module [%s]: Failed to register an [adapter] extension of point [adapters].",
                                                                extension.getDeclaringModule().getSymbolicName()), e);
                }
            }
        }
    }

    private void registerAdapter(Extension extension, ConfigurationElement child) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Module declaringModule = extension.getDeclaringModule();
        String extensibleTypeName = getChildValue(child, "extensibleType");
        String extensionFactoryName = getChildValue(child, "extensionFactory");
        String extensionTypeName = getChildValue(child, "extensionType");
        String extensionSubTypeName = getChildValue(child, "extensionSubType");
        if (!isNullOrEmpty(extensibleTypeName)) {
            if (!isNullOrEmpty(extensionFactoryName)) {
                Class<?> extensibleType = declaringModule.loadClass(extensibleTypeName);
                ExtensionFactory extensionFactory = (ExtensionFactory) declaringModule.loadClass(extensionFactoryName).newInstance();
                registerAdapter(declaringModule, extensibleType, extensionFactory);
            } else if (!isNullOrEmpty(extensionTypeName)) {
                if (!isNullOrEmpty(extensionSubTypeName)) {
                    Class<?> extensibleType = declaringModule.loadClass(extensibleTypeName);
                    Class<?> extensionType = declaringModule.loadClass(extensionTypeName);
                    Class<?> extensionSubType = declaringModule.loadClass(extensionSubTypeName);
                    registerAdapter(declaringModule, extensibleType, new SingleTypeExtensionFactory(extensionType, extensionSubType));
                } else {
                    Class<?> extensibleType = declaringModule.loadClass(extensibleTypeName);
                    Class<?> extensionType = declaringModule.loadClass(extensionTypeName);
                    registerAdapter(declaringModule, extensibleType, new SingleTypeExtensionFactory(extensionType));
                }
            } else {
                moduleContext.getLogger().severe(
                        String.format("Module [%s]: Missing either element 'extensionFactory' or 'extensionType' in an extension of point [adapters].", declaringModule.getSymbolicName()));
            }
        } else {
            moduleContext.getLogger().severe(
                    String.format("Module [%s]: Missing element 'extensibleType' in an extension of point [adapters].", declaringModule.getSymbolicName()));
        }
    }

    private void registerAdapter(Module declaringModule, Class<?> extensibleType, ExtensionFactory extensionFactory) {
        ExtensionManager.getInstance().register(extensibleType, extensionFactory);
        moduleContext.getLogger().info(String.format("Module [%s]: Adapter registered: A [%s] can now be extended to %s",
                                                     declaringModule.getSymbolicName(),
                                                     extensibleType,
                                                     Arrays.toString(extensionFactory.getExtensionTypes())));
    }

    private String getChildValue(ConfigurationElement child, String elementName) {
        ConfigurationElement element = child.getChild(elementName);
        return element != null ? element.getValue() : null;
    }

    private boolean isNullOrEmpty(String extendibleTypeName) {
        return extendibleTypeName == null || extendibleTypeName.trim().length() == 0;
    }

    public static String[] parseSpiConfiguration(URL resource) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
            ArrayList<String> classNames = new ArrayList<String>(3);
            while (true) {
                String s = bufferedReader.readLine();
                if (s == null) {
                    break;
                }
                int i = s.indexOf('#');
                if (i >= 0) {
                    s = s.substring(0, i);
                }
                s = s.trim();
                if (s.length() > 0) {
                    classNames.add(s);
                }
            }
            return classNames.toArray(new String[classNames.size()]);
        }
    }

    private static class NullClassLoader extends ClassLoader {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }

        @Override
        protected URL findResource(String name) {
            return null;
        }

        @Override
        protected String findLibrary(String libname) {
            return null;
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            return new Vector<URL>().elements();
        }
    }
}
