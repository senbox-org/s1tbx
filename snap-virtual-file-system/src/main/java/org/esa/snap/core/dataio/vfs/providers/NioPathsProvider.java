package org.esa.snap.core.dataio.vfs.providers;

import org.esa.snap.core.dataio.NioPaths;
import org.esa.snap.core.vfsspi.VFSProvider;

import java.util.Arrays;

/**
 * Paths Provider for VFS.
 * VFS Service provider for access NioPaths methods from outside of snap-virtual-file-system module via SPI.
 *
 * @author Adrian Drăghici
 */
public final class NioPathsProvider implements VFSProvider {

    /**
     * The name of service provider method which tells whether if given path is the root of a tree in the VFS.
     */
    private static final String SERVICE_METHOD_IS_VIRTUAL_FILESYSTEM_PATH = "isVirtualFileSystemPath";

    /**
     * The name of service provider method which converts a path string, or a sequence of strings that when joined form a path string, to a {@code Path}.
     */
    private static final String SERVICE_METHOD_GET = "get";

    /**
     * Runs the service provider method specified by the given absolute name (package.class.method), with specified parameters and return the result converted to specified type.
     * The service provider method can be 'isVirtualFileSystemPath' or 'get'.
     *
     * @param serviceMethodName The absolute name of service provider method
     * @param returnType        The service provider method return result type
     * @param params            The service provider method parameters
     * @param <R>               The generic type of service provider method return result: can be {@code Boolean} or {@code Path}
     * @return The result of the service provider method execution
     */
    public <R> R runVFSService(String serviceMethodName, Class<? extends R> returnType, Object... params) {
        switch (serviceMethodName) {
            case SERVICE_METHOD_IS_VIRTUAL_FILESYSTEM_PATH:
                if (params.length == 1) {
                    return returnType.cast(NioPaths.isVirtualFileSystemPath((String) params[0]));
                }
                break;
            case SERVICE_METHOD_GET:
                if (params.length > 0) {
                    return returnType.cast(NioPaths.get((String) params[0], Arrays.copyOfRange(params, 1, params.length, String[].class)));
                }
                break;
            default:
                break;
        }
        return null;
    }

}
