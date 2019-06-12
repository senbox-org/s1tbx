package org.esa.snap.remote.execution.file.system;

import java.io.IOException;

/**
 * Created by jcoravu on 23/5/2019.
 */
public class MacLocalMachineFileSystem extends UnixLocalMachineFileSystem {

    public MacLocalMachineFileSystem() {
        super();
    }

    @Override
    public String findPhysicalSharedFolderPath(String shareNameToFind, String localPassword) throws IOException {
        return null;
    }
}
