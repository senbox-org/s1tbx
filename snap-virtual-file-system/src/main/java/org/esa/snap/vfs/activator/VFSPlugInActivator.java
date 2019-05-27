package org.esa.snap.vfs.activator;

import org.esa.snap.runtime.Activator;
import org.esa.snap.vfs.VFS;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepositoriesController;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin activator for VFS
 *
 * @author Jean Coravu
 * @author Adrian Drăghici
 */
public class VFSPlugInActivator implements Activator {

    private static Logger logger = Logger.getLogger(VFSPlugInActivator.class.getName());

    /**
     * Creates the new plugin activator for VFS.
     */
    public VFSPlugInActivator() {
        //nothing to do
    }

    /**
     * Starts the VFS plugin by initializing VFS providers with configurations stored in SNAP config files.
     */
    @Override
    public void start() {
        try {
            Path configFile = VFSRemoteFileRepositoriesController.getDefaultConfigFilePath();
            List<VFSRemoteFileRepository> vfsRepositories = VFSRemoteFileRepositoriesController.getVFSRemoteFileRepositories(configFile);
            VFS.getInstance().initRemoteInstalledProviders(vfsRepositories);
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Unable to start the VFS Plugin. Reason: " + exception.getMessage(), exception);
        }
    }

    /**
     * Stops the VFS plugin
     */
    @Override
    public void stop() {
        // nothing to do
    }
}
