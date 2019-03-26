package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

/**
 * Created by jcoravu on 21/3/2019.
 */
public class SwiftFileSystem extends AbstractRemoteFileSystem {
    /**
     * Creates the new File System for Object Storage VFS.
     *
     * @param provider  The VFS provider
     */
    public SwiftFileSystem(AbstractRemoteFileSystemProvider provider, String fileSystemRoot) {
        super(provider, fileSystemRoot);
    }
}
