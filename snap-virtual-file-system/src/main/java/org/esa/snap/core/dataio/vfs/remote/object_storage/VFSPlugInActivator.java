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
        } else {
            logger.log(Level.SEVERE, "VFS not loaded.");
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
//            URL target = VFSPlugInActivator.class.getProtectionDomain().getCodeSource().getLocation();
//            method.invoke(classLoader, target);
//            if (isLoadedVFS()) {
//                logger.log(Level.FINE, "VFS loaded successfully.");
//            } else {
//                throw new ExceptionInInitializerError("VFS not loaded.");
//            }
//        } catch (Exception ex) {
//            logger.log(Level.SEVERE, "Unable to load VFS.\nDetails:" + ex.getMessage());
//        }
//    }

    private boolean isLoadedVFS() {
        List<FileSystemProvider> providers = ObjectStorageFileSystemProvider.installedProviders();
        for (FileSystemProvider provider : providers) {
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
        FileSystem fs;
        try {
            uri = new URI(vfsRemoteFileRepository.getSchema() + vfsRemoteFileRepository.getAddress());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to create URL for VFS. Details: " + ex.getMessage());
            throw new IOException(ex);
        }
        Map<String, String> connectionData = new HashMap<>();
        connectionData.put(ROOT_PROPERTY_NAME, getRootPath(vfsRemoteFileRepository.getName()));
        for (Property vfsRemoteFileRepositoryProperty : vfsRemoteFileRepository.getProperties()) {
            connectionData.put(vfsRemoteFileRepositoryProperty.getName(), vfsRemoteFileRepositoryProperty.getValue());
        }
        try {
            fs = ObjectStorageFileSystem.getFileSystem(uri);
        } catch (Exception ex) {
            logger.log(Level.FINE, "VFS not loaded. Details: " + ex.getMessage());
            try {
                fs = ObjectStorageFileSystem.newFileSystem(uri, null);
            } catch (Exception ex1) {
                logger.log(Level.SEVERE, "Unable to initialize VFS. Details: " + ex.getMessage());
                throw new IOException(ex1);
            }
        }
        FileSystemProvider provider = fs.provider();
        if (provider instanceof ObjectStorageFileSystemProvider) {
            ((ObjectStorageFileSystemProvider) provider).setConnectionData(vfsRemoteFileRepository.getAddress(), connectionData);
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
