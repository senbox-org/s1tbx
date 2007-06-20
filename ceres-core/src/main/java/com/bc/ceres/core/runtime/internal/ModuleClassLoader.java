package com.bc.ceres.core.runtime.internal;

import java.net.URLClassLoader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Collections;
import java.util.Iterator;
import java.io.IOException;

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

    public ModuleClassLoader(ClassLoader[] delegates,
                             URL[] dependencyUrls,
                             URL[] nativeUrls,
                             ClassLoader parent) {
        super(dependencyUrls, parent);
        this.nativeUrls = nativeUrls;
        this.delegates = delegates;
    }

    @Override
    public String findLibrary(String libname) {
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
            String path = ((ModuleClassLoader) parent).findLibrary(libname);
            if (path != null) {
                return path;
            }
        }
        return super.findLibrary(libname);
    }

    @Override
    public URL findResource(final String name) {
        URL resource = super.findResource(name);
        if (resource != null) {
            return resource;
        }
        for (ClassLoader delegate : delegates) {
            resource = delegate.getResource(name);
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

        final HashSet<URL> urls = new HashSet<URL>(Collections.list(resources));
        for (ClassLoader delegate : delegates) {
            urls.addAll(Collections.list(delegate.getResources(name)));
        }

        final Iterator<URL> iterator = urls.iterator();
        return new Enumeration<URL>() {
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            public URL nextElement() {
                return iterator.next();
            }
        };
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException cne) {
            for (ClassLoader delegate : delegates) {
                try {
                    return delegate.loadClass(name);
                } catch (ClassNotFoundException e) {
                    // ignore, we already have a ClassNotFoundException = cne
                }
            }
            throw cne;
        }
    }

    public ClassLoader[] getDelegates() {
        return delegates;
    }

    public URL[] getNativeUrls() {
        return nativeUrls;
    }

}
