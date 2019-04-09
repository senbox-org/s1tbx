package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

/**
 * File System for Swift
 *
 * @author Jean Coravu
 */
class SwiftFileSystem extends AbstractRemoteFileSystem {

    /**
     * Creates the new File System for VFS.
     *
     * @param provider The VFS provider
     */
    SwiftFileSystem(AbstractRemoteFileSystemProvider provider, String fileSystemRoot) {
        super(provider, fileSystemRoot);
    }
}
