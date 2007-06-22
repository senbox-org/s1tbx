package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ExtensionPoint;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.runtime.RuntimeRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class RuntimeActivator implements Activator {

    private static RuntimeActivator instance;

    private Map<String, RuntimeRunnable> applications;
    private List<ServiceRegistration> serviceRegistrations;
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
        initApplications();
        initServiceProviders();
    }

    public void stop(ModuleContext moduleContext) throws CoreException {
        disposeServiceProviders();
        disposeApplications();
        this.moduleContext = null;
    }


    private void initServiceProviders() {
        serviceRegistrations = new ArrayList<ServiceRegistration>(32);
        ExtensionPoint extensionPoint = moduleContext.getModule().getExtensionPoint("serviceProviders");
        Extension[] extensions = extensionPoint.getExtensions();
        for (int i = 0; i < extensions.length; i++) {
            Extension extension = extensions[i];

            ConfigurationElement[] children = extension.getConfigurationElement().getChildren("serviceProvider");
            for (ConfigurationElement child : children) {
                String providerClassName = child.getValue();
                Module declaringModule = extension.getDeclaringModule();
                Class<?> providerClass = null;
                try {
                    providerClass = declaringModule.loadClass(providerClassName);
                } catch (Throwable t) {
                    moduleContext.getLogger().log(Level.SEVERE,
                                                  String.format("Failed to load service provider [%s].",
                                                                providerClassName), t);
                }
                if (providerClass != null) {
                    Set<ServiceRegistration> serviceRegistrations = getServiceRegistrations(providerClass);
                    for (ServiceRegistration serviceRegistration : serviceRegistrations) {
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
                        if (providerImplClassNames != null) {
                            for (int j = 0; j < providerImplClassNames.length; j++) {
                                String providerImplClassName = providerImplClassNames[j];
                                Class<?> providerImplClass = null;
                                try {
                                    providerImplClass = serviceRegistration.module.loadClass(providerImplClassName);
                                } catch (Throwable t) {
                                    // todo - log problem
                                }
                                if (providerImplClass != null) {
                                    Object providerImpl = null;
                                    if (providerClass.isAssignableFrom(providerImplClass)) {
                                        try {
                                            providerImpl = providerImplClass.newInstance();
                                        } catch (Throwable t) {
                                            // todo - log problem
                                        }
                                        if (providerImpl != null) {
                                            serviceRegistration.serviceRegistry.addService(providerImpl);
                                            serviceRegistration.providerImpl = providerImpl;
                                            moduleContext.getLogger().info(
                                                    "Service " + providerImplClass + " registered");
                                            this.serviceRegistrations.add(serviceRegistration);
                                        }
                                    } else {
                                        // todo - log problem
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Set<ServiceRegistration> getServiceRegistrations(Class<?> providerClass) {
        ServiceRegistry serviceRegistry = ServiceRegistryFactory.getInstance().getServiceRegistry(providerClass);
        String resourcePath = "META-INF/services/" + providerClass.getName();
        Module[] modules = moduleContext.getModules();
        HashSet<ServiceRegistration> serviceRegistrations = new HashSet<ServiceRegistration>(10);
        for (int j = 0; j < modules.length; j++) {
            Module module = modules[j];
            Enumeration<URL> resources = null;
            try {
                resources = module.getResources(resourcePath);
            } catch (IOException e) {
                moduleContext.getLogger().log(Level.SEVERE,
                                              String.format(
                                                      "Failed to load configuration [%s] from module [%s].",
                                                      resourcePath, module.getName()), e);
            }
            if (resources != null) {
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    serviceRegistrations.add(new ServiceRegistration(url, module, serviceRegistry));
                }
            }
        }
        return serviceRegistrations;
    }

    private static class ServiceRegistration {

        URL url;
        Module module;
        ServiceRegistry serviceRegistry;
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
            moduleContext.getLogger().info("Service " + serviceRegistration.providerImpl.getClass() + " unregistered");
        }
        serviceRegistrations.clear();
    }


    private void initApplications() {
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
                    String msg = String.format("Application [%s] registered (declared by module [%s]).", appId,
                                               declaringModule.getSymbolicName());
                    moduleContext.getLogger().info(msg);
                }
            }
        }
    }

    private void disposeApplications() {
        applications.clear();
        applications = null;
    }

    public static String[] parseSpiConfiguration(URL resource) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource.openStream()));
        try {
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
                if (!s.isEmpty()) {
                    classNames.add(s);
                }
            }
            return classNames.toArray(new String[classNames.size()]);
        } finally {
            bufferedReader.close();
        }
    }
}
