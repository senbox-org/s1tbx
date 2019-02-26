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

    private static final String SERVICE_METHOD_IS_VIRTUAL_FILESYSTEM_PATH = "isVirtualFileSystemPath";
    private static final String SERVICE_METHOD_GET = "get";

    public NioPathsProvider() {

    }

    public <R> R runVFSService(String serviceMethodName, Class<? extends R> returnType, Object... params) {
        switch (serviceMethodName) {
            case SERVICE_METHOD_IS_VIRTUAL_FILESYSTEM_PATH:
                if (params.length == 1) {
                    return returnType.cast(NioPaths.isVirtualFileSystemPath((String) params[0]));
                }
                break;
            case SERVICE_METHOD_GET: {
                if (params.length > 0) {
                    return returnType.cast(NioPaths.get((String) params[0], Arrays.copyOfRange(params, 1, params.length, String[].class)));
                }
                break;
            }
        }
        return null;
    }

}
