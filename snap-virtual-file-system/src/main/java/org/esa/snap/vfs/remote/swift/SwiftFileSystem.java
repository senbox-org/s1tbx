package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

/**
 * File System for Swift
 *
 * @author Jean Coravu
 */
class SwiftFileSystem extends AbstractRemoteFileSystem {

    private final String fileSystemRoot;

    /**
     * Creates the new File System for VFS.
     *
     * @param provider The VFS provider
     */
    SwiftFileSystem(AbstractRemoteFileSystemProvider provider, String fileSystemRoot) {
        super(provider);
        this.fileSystemRoot = fileSystemRoot;
    }

    @Override
    protected String getFileSystemRoot() {
        return this.fileSystemRoot;
    }
}
