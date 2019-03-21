package org.esa.snap.core.vfsspi;

import java.util.ServiceLoader;

/**
 * Service Provider Interface for VFS.
 * Provide access to VFS modules internal methods using SPI via VFS Service Providers for prevent cyclic graphs caused by direct dependency to VFS modules from SNAP modules.
 *
 * @author Adrian Drăghici
 */
public interface VFSProvider {

    /**
     * Runs the service provider method specified by the given absolute name (package.class.method), with specified parameters and return the result converted to specified type.
     *
     * @param serviceMethodName The absolute name of service provider method
     * @param returnType        The service provider method return result type
     * @param params            The service provider method parameters
     * @param <R>               The generic type of service provider method return result
     * @return The result of the service provider method execution
     */
    <R> R runVFSService(String serviceMethodName, Class<? extends R> returnType, Object... params);

    /**
     * Creates the new Service Provider for VFS instance specified by the given absolute name of service provider class (package.class).
     *
     * @param vfsProviderClassName The absolute name of service provider class (package.class)
     * @return The new Service Provider for VFS instance
     * @throws ClassNotFoundException If the class was not found
     */
    static VFSProvider getVFSProviderInstance(String vfsProviderClassName) throws ClassNotFoundException {
        VFSProvider vfsProvider = null;
        for (VFSProvider instance : ServiceLoader.load(VFSProvider.class, ClassLoader.getSystemClassLoader())) {
            vfsProvider = instance.getClass().getName().contentEquals(vfsProviderClassName) ? instance : vfsProvider;
        }
        if (vfsProvider == null) {
            throw new ClassNotFoundException("Class: " + vfsProviderClassName + " is not a provider of this service!");
        }
        return vfsProvider;
    }

}
