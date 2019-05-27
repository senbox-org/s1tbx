package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;

/**
 * File System for HTTP
 *
 * @author Jean Coravu
 */
class HttpFileSystem extends AbstractRemoteFileSystem {

    /**
     * Creates the new File System for VFS.
     *
     * @param provider The VFS provider
     */
    HttpFileSystem(HttpFileSystemProvider provider, String root) {
        super(provider, root);
    }
}
