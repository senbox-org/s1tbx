package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

/**
 * File System for S3
 *
 * @author Jean Coravu
 */
class S3FileSystem extends AbstractRemoteFileSystem {

    private final String fileSystemRoot;

    /**
     * Creates the new File System for VFS.
     *
     * @param provider The VFS provider
     */
    S3FileSystem(AbstractRemoteFileSystemProvider provider, String fileSystemRoot) {
        super(provider);
        this.fileSystemRoot = fileSystemRoot;
    }

    @Override
    protected String getFileSystemRoot() {
        return this.fileSystemRoot;
    }
}
