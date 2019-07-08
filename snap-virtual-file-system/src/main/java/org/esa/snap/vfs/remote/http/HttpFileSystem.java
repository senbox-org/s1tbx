package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;

/**
 * File System for HTTP
 *
 * @author Jean Coravu
 */
class HttpFileSystem extends AbstractRemoteFileSystem {

    private final String fileSystemRoot;

    /**
     * Creates the new File System for VFS.
     *
     * @param provider The VFS provider
     */
    HttpFileSystem(HttpFileSystemProvider provider, String fileSystemRoot) {
        super(provider);
        this.fileSystemRoot = fileSystemRoot;
    }

    @Override
    protected String getFileSystemRoot() {
        return this.fileSystemRoot;
    }
}
