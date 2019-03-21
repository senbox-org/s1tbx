package org.esa.snap.core.dataio.vfs.remote.object_storage;

import org.esa.snap.core.dataio.vfs.remote.model.Property;
import org.esa.snap.core.dataio.vfs.remote.model.VFSRemoteFileRepository;
import org.esa.snap.runtime.Activator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
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
        try {
            initVFS();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to initialize VFS. Details: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void stop() {
        //nothing to do
    }

    private void initVFS() {
        List<VFSRemoteFileRepository> vfsRemoteFileRepositories = VFSRemoteFileRepositoriesController.getVFSRemoteFileRepositories();
        if (vfsRemoteFileRepositories.isEmpty()) {
            throw new IllegalArgumentException("Remote file repositories not defined.");
        }
        for (VFSRemoteFileRepository vfsRemoteFileRepository : vfsRemoteFileRepositories) {
            try {
                initAndGetVFS(vfsRemoteFileRepository);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unable to init VFS. Details: " + ex.getMessage());
            }
        }
    }

    public static FileSystem initAndGetVFS(VFSRemoteFileRepository vfsRemoteFileRepository) throws URISyntaxException, IOException {
        Map<String, String> connectionData = new HashMap<>();
        connectionData.put(ROOT_PROPERTY_NAME, getRootPath(vfsRemoteFileRepository.getName()));
        for (Property vfsRemoteFileRepositoryProperty : vfsRemoteFileRepository.getProperties()) {
            connectionData.put(vfsRemoteFileRepositoryProperty.getName(), vfsRemoteFileRepositoryProperty.getValue());
        }
        List<FileSystemProvider> providers = ObjectStorageFileSystemProvider.installedProviders();
        for (FileSystemProvider provider : providers) {
            if (vfsRemoteFileRepository.getSchema().startsWith(provider.getScheme()) && provider instanceof ObjectStorageFileSystemProvider) {
                ((ObjectStorageFileSystemProvider) provider).setConnectionData(vfsRemoteFileRepository.getAddress(), connectionData);
                URI uri = new URI(vfsRemoteFileRepository.getSchema() + vfsRemoteFileRepository.getAddress());
                try {
                    return ObjectStorageFileSystem.getFileSystem(uri);
                } catch (Exception ex) {
                    return ObjectStorageFileSystem.newFileSystem(uri, connectionData);
                }
            }
        }
        throw new FileSystemNotFoundException("VFS with schema: " + vfsRemoteFileRepository.getSchema() + " not found");
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
