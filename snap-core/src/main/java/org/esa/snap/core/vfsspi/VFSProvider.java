package org.esa.snap.core.vfsspi;

import java.util.ServiceLoader;

/**
 * Service Provider Interface for VFS.
 * Provide access to VFS modules internal methods using SPI via VFS Service Providers for prevent cyclic graphs caused by direct dependency to VFS modules from SNAP modules.
 *
 * @author Adrian Drăghici
 */
public interface VFSProvider {

    <R> R runVFSService(String serviceMethodName, Class<? extends R> returnType, Object... params);

    static VFSProvider getVFSProviderInstance(String vfsProviderClassName) throws ClassNotFoundException {
        VFSProvider nioFileSystemViewInstance = null;
        for (VFSProvider instance : ServiceLoader.load(VFSProvider.class)) {
            nioFileSystemViewInstance = instance.getClass().getName().contentEquals(vfsProviderClassName) ? instance : nioFileSystemViewInstance;
        }
        if (nioFileSystemViewInstance == null) {
            throw new ClassNotFoundException("Class: " + vfsProviderClassName + " is not a provider of this service!");
        }
        return nioFileSystemViewInstance;
    }

}
