package com.bc.ceres.core.runtime.internal;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class loader used for modules. It adds the following features to the {@link URLClassLoader}.
 * <ol>
 * <li>Besides a parent class loader, it can have multiple delegate class loaders.</li>
 * <li>It can find and load native libraries from arbitrary locations.</li>
 * </ol>
 */
class ModuleClassLoader extends URLClassLoader {

    private ClassLoader[] delegates;
    private URL[] nativeUrls;
    private Map<ClassLoader, Map<String, List<URL>>> resolvedResources;

    public ModuleClassLoader(ClassLoader[] delegates,
                             URL[] dependencyUrls,
                             URL[] nativeUrls,
                             ClassLoader parent) {
        super(dependencyUrls, parent);
        this.nativeUrls = nativeUrls;
        this.delegates = delegates;
        resolvedResources = new HashMap<ClassLoader, Map<String, List<URL>>>();
    }

    @Override
    protected String findLibrary(String libname) {
        for (URL url : nativeUrls) {
            if (url.toExternalForm().endsWith(System.mapLibraryName(libname))) {
                return FileHelper.urlToFile(url).getAbsolutePath();
            }
        }
        for (ClassLoader classLoader : delegates) {
            if (classLoader instanceof ModuleClassLoader) {
                String path = ((ModuleClassLoader) classLoader).findLibrary(libname);
                if (path != null) {
                    return path;
                }
            }
        }
        ClassLoader parent = getParent();
        if (parent instanceof ModuleClassLoader) {
            // Must cast, otherwise we cannot call protected ClassLoader.findLibrary() method
            String path = ((ModuleClassLoader) parent).findLibrary(libname);
            if (path != null) {
                return path;
            }
        }
        return super.findLibrary(libname);
    }

    @Override
    public URL findResource(final String name) {
        final URL localResource = super.findResource(name);
        if(localResource != null) {
            return localResource;
        }
        for (ClassLoader delegate : delegates) {
            URL resource = delegate.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }    

    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {

        final Enumeration<URL> resources = super.findResources(name);
        if (delegates.length == 0) {
            return resources;
        }

        final Set<URL> urls = new HashSet<URL>(Collections.list(resources));
        for (ClassLoader delegate : delegates) {
            final Map<String, List<URL>> resourceMap = getResourceMap(delegate);
            if(resourceMap.containsKey(name)) {
                urls.addAll(resourceMap.get(name));
            }else {
                final List<URL> urlArrayList = Collections.list(delegate.getResources(name));
                urls.addAll(urlArrayList);
                resourceMap.put(name, urlArrayList);
            }
        }
        return Collections.enumeration(urls) ;
    }

    private Map<String, List<URL>> getResourceMap(ClassLoader delegate) {
        Map<String, List<URL>> resourceMap;
        if(resolvedResources.containsKey(delegate)) {
            resourceMap = resolvedResources.get(delegate);
        }else {
            resourceMap = new HashMap<String, List<URL>>();
            resolvedResources.put(delegate, resourceMap);
        }
        return resourceMap;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            for (ClassLoader delegate : delegates) {
                try {
                    return delegate.loadClass(name);
                } catch (ClassNotFoundException e2) {
                    // ignore, we still can try more
                }
            }
            throw e;
        }
    }

    public ClassLoader[] getDelegates() {
        return delegates;
    }

    public URL[] getNativeUrls() {
        return nativeUrls;
    }

}
