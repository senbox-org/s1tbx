package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;

import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by jcoravu on 21/3/2019.
 */
public class HttpFileSystem extends AbstractRemoteFileSystem {
    /**
     * Creates the new File System for Object Storage VFS.
     *
     * @param provider  The VFS provider
     */
    public HttpFileSystem(HttpFileSystemProvider provider, String root) {
        super(provider, root);
    }
}
