package org.esa.snap.vfs.remote;

/**
 * Created by jcoravu on 17/5/2019.
 */
public class RegularFileMetadata {

    private final String fileURL;
    private final long size;
    private final String lastModified;

    RegularFileMetadata(String lastModified, long size) {
        this.fileURL = null;
        this.size = size;
        this.lastModified = lastModified;
    }

    RegularFileMetadata(String fileURL, String lastModified, long size) {
        this.fileURL = fileURL;
        this.size = size;
        this.lastModified = lastModified;
    }

    public String getFileURL() {
        return this.fileURL;
    }

    public long getSize() {
        return this.size;
    }

    public String getLastModified() {
        return this.lastModified;
    }
}
