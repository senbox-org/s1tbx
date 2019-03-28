package org.esa.snap.vfs.activator;

import org.esa.snap.runtime.Activator;
import org.esa.snap.vfs.VFS;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepositoriesController;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VFSPlugInActivator implements Activator {

    private static Logger logger = Logger.getLogger(VFSPlugInActivator.class.getName());

    public VFSPlugInActivator() {
    }

    @Override
    public void start() {
        try {
            Path configFile = VFSRemoteFileRepositoriesController.getDefaultConfigFilePath();
            List<VFSRemoteFileRepository> vfsRepositories = VFSRemoteFileRepositoriesController.getVFSRemoteFileRepositories(configFile);
            VFS.getInstance().initRemoteInstalledProviders(vfsRepositories);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }

    @Override
    public void stop() {
        // nothing to do
    }
}
