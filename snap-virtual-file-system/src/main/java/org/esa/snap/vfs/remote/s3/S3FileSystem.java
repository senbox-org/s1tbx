package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

/**
 * Created by jcoravu on 21/3/2019.
 */
public class S3FileSystem extends AbstractRemoteFileSystem {
    /**
     * Creates the new File System for Object Storage VFS.
     *
     * @param provider  The VFS provider
     */
    public S3FileSystem(AbstractRemoteFileSystemProvider provider, String fileSystemRoot) {
        super(provider, fileSystemRoot);
    }
}
