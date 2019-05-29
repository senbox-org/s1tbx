package org.esa.snap.vfs.remote;

/**
 * Created by jcoravu on 17/5/2019.
 */
public class RegularFileMetadata {

    private final long size;
    private final String lastModified;

    public RegularFileMetadata(String lastModified, long size) {
        this.size = size;
        this.lastModified = lastModified;
    }

    public long getSize() {
        return size;
    }

    public String getLastModified() {
        return lastModified;
    }
}
