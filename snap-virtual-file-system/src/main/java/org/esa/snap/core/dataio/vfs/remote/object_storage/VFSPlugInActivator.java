package org.esa.snap.core.dataio.vfs.remote.object_storage;

import org.esa.snap.core.dataio.vfs.remote.model.Property;
import org.esa.snap.core.dataio.vfs.remote.model.VFSRemoteFileRepository;
import org.esa.snap.runtime.Activator;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VFSPlugInActivator implements Activator {

    /**
     * The name of root property, used on VFS instance creation parameters.
     */
    private static final String ROOT_PROPERTY_NAME = "root";

    /**
     * The pattern for name.
     */
    private static final String ROOT_NAME = "%root_name%";

    /**
     * The path name of root for showing on file chooser.
     */
    private static final String ROOT_PATH = ROOT_NAME + ":/";

    private static Logger logger = Logger.getLogger(VFSPlugInActivator.class.getName());

    @Override
    public void start() {
        if (isLoadedVFS()) {
            initVFS();
        }
    }

    @Override
    public void stop() {
        //nothing to do
    }
//
//    private void loadVFS() {
//        logger.log(Level.FINE, "Loading VFS.");
//        try {
//            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
//            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
//            method.setAccessible(true);
//            method.invoke(classLoader, VFSPlugInActivator.class.getProtectionDomain().getCodeSource().getLocation());
//            if (!isLoadedVFS()) {
//                throw new ExceptionInInitializerError("VFS not loaded.");
//            }
//        } catch (Exception ex) {
//            logger.log(Level.SEVERE, "Unable to load VFS.\nDetails:" + ex.getMessage());
//        }
//    }

    private boolean isLoadedVFS() {
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (provider instanceof ObjectStorageFileSystemProvider) {
                return true;
            }
        }
        return false;
    }

    private void initVFS() {
        try {
            List<VFSRemoteFileRepository> vfsRemoteFileRepositories = VFSRemoteFileRepositoriesController.getVFSRemoteFileRepositories();
            if (vfsRemoteFileRepositories.isEmpty()) {
                throw new IllegalArgumentException("Remote file repositories not defined.");
            }
            for (VFSRemoteFileRepository vfsRemoteFileRepository : vfsRemoteFileRepositories) {
                initAndGetVFS(vfsRemoteFileRepository);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to init VFS. Details: " + ex.getMessage());
        }
    }


    public static FileSystem initAndGetVFS(VFSRemoteFileRepository vfsRemoteFileRepository) throws IOException {
        URI uri;
        FileSystem fs = null;
        try {
            uri = new URI(vfsRemoteFileRepository.getSchema() + vfsRemoteFileRepository.getAddress());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to create URL for VFS. Details: " + ex.getMessage());
            throw new IOException(ex);
        }
        try {
            fs = FileSystems.getFileSystem(uri);
        } catch (Exception ex) {
            logger.log(Level.FINE, "VFS not loaded. Details: " + ex.getMessage());
        } finally {
            if (fs != null) {
                fs.close();
            }
        }
        try {
            Map<String, String> env = new HashMap<>();
            env.put(ROOT_PROPERTY_NAME, getRootPath(vfsRemoteFileRepository.getName()));
            for (Property vfsRemoteFileRepositoryProperty : vfsRemoteFileRepository.getProperties()) {
                env.put(vfsRemoteFileRepositoryProperty.getName(), vfsRemoteFileRepositoryProperty.getValue());
            }
            uri = new URI(vfsRemoteFileRepository.getSchema() + vfsRemoteFileRepository.getAddress());
            fs = FileSystems.newFileSystem(uri, env);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to initialize VFS. Details: " + ex.getMessage());
            throw new IOException(ex);
        }
        return fs;
    }

    /**
     * Gets the path name of root, using given name of root.
     *
     * @param root The name of root
     * @return The path name of root
     */
    private static String getRootPath(String root) {
        return ROOT_PATH.replace(ROOT_NAME, root);
    }

}
