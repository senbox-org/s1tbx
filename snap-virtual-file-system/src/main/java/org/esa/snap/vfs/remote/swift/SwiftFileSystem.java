package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.ObjectStorageFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

/**
 * Created by jcoravu on 21/3/2019.
 */
public class SwiftFileSystem extends ObjectStorageFileSystem {
    /**
     * Creates the new File System for Object Storage VFS.
     *
     * @param provider  The VFS provider
     * @param address   The VFS service address
     * @param separator The VFS path separator
     */
    public SwiftFileSystem(AbstractRemoteFileSystemProvider provider, String address, String separator) {
        super(provider, address, separator);
    }
}
