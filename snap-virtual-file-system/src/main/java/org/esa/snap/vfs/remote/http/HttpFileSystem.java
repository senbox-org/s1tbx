package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.esa.snap.vfs.remote.ObjectStorageFileSystem;

/**
 * Created by jcoravu on 21/3/2019.
 */
public class HttpFileSystem extends ObjectStorageFileSystem {
    /**
     * Creates the new File System for Object Storage VFS.
     *
     * @param provider  The VFS provider
     * @param address   The VFS service address
     * @param separator The VFS path separator
     */
    public HttpFileSystem(AbstractRemoteFileSystemProvider provider, String address, String separator) {
        super(provider, address, separator);
    }
}
