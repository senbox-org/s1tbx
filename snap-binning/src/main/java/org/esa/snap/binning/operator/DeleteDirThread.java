package org.esa.snap.binning.operator;

import org.esa.snap.core.util.io.FileUtils;

import java.io.File;

/**
 * Used for registering a shutdown hook to the runtime and deleting files when
 * the VM exits.
 *
 * @author Marco Peters
 * @see Runtime#addShutdownHook(Thread)
 */
class DeleteDirThread extends Thread {


    private final File tempDir;

    DeleteDirThread(File tempDir) {
        this.tempDir = tempDir;
    }

    @Override
    public void run() {
        FileUtils.deleteTree(tempDir);
    }
}
