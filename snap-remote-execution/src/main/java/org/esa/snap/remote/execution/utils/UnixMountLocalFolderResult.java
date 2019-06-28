package org.esa.snap.remote.execution.utils;

/**
 * Created by jcoravu on 23/5/2019.
 */
public class UnixMountLocalFolderResult {

    private boolean sharedFolderCreated;
    private boolean sharedFolderMounted;

    public UnixMountLocalFolderResult() {
        this.sharedFolderCreated = false;
        this.sharedFolderMounted = false;
    }

    public void setSharedFolderMounted(boolean sharedFolderMounted) {
        this.sharedFolderMounted = sharedFolderMounted;
    }

    public boolean isSharedFolderMounted() {
        return sharedFolderMounted;
    }

    public void setSharedFolderCreated(boolean sharedFolderCreated) {
        this.sharedFolderCreated = sharedFolderCreated;
    }

    public boolean isSharedFolderCreated() {
        return sharedFolderCreated;
    }
}
