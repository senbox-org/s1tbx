package org.esa.snap.vfs;

import org.esa.snap.vfs.preferences.model.Property;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Created by jcoravu on 21/3/2019.
 */
public class VFS {

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

    private List<FileSystemProvider> installedProviders;

    private final static VFS instance;
    static {
        instance = new VFS();
        instance.loadInstalledProviders();
    }

    private VFS() {
    }

    public static VFS getInstance() {
        return instance;
    }

    public FileSystemProvider getFileSystemProviderByScheme(String scheme) {
        for (int i = 0; i<this.installedProviders.size(); i++) {
            FileSystemProvider fileSystemProvider = this.installedProviders.get(i);
            if (scheme.equalsIgnoreCase(fileSystemProvider.getScheme())) {
                return fileSystemProvider;
            }
        }
        return null;
    }

    public void initRemoteInstalledProviders(List<VFSRemoteFileRepository> vfsRepositories) {
        for (int i = 0; i<this.installedProviders.size(); i++) {
            FileSystemProvider fileSystemProvider = this.installedProviders.get(i);

            if (fileSystemProvider instanceof AbstractRemoteFileSystemProvider) {
                AbstractRemoteFileSystemProvider remoteFileSystemProvider = (AbstractRemoteFileSystemProvider)fileSystemProvider;

                VFSRemoteFileRepository foundRepository = null;
                for (int k=0; k<vfsRepositories.size() && foundRepository == null; k++) {
                    VFSRemoteFileRepository repository = vfsRepositories.get(k);
                    if (repository.getScheme().equalsIgnoreCase(remoteFileSystemProvider.getScheme())) {
                        foundRepository = repository;
                    }
                }

                if (foundRepository != null) {
                    Map<String, String> connectionData = new HashMap<>();
                    connectionData.put(ROOT_PROPERTY_NAME, getRootPath(foundRepository.getName()));
                    for (Property vfsRemoteFileRepositoryProperty : foundRepository.getProperties()) {
                        connectionData.put(vfsRemoteFileRepositoryProperty.getName(), vfsRemoteFileRepositoryProperty.getValue());
                    }
                    remoteFileSystemProvider.setConnectionData(foundRepository.getAddress(), connectionData);
                }
            }
        }
    }

    public Path getPath(URI uri) {
        String scheme =  uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("Missing scheme.");
        }
        // check for default provider to avoid loading of installed providers
        if (scheme.equalsIgnoreCase("file")) {
            return FileSystems.getDefault().provider().getPath(uri);
        }
        for (FileSystemProvider provider: this.installedProviders) {
            if (provider.getScheme().equalsIgnoreCase(scheme)) {
                return provider.getPath(uri);
            }
        }
        throw new FileSystemNotFoundException("The file system provider with the scheme '" + scheme + "' is not installed.");
    }

    private static String getRootPath(String root) {
        return ROOT_PATH.replace(ROOT_NAME, root);
    }

    private void loadInstalledProviders() {
        this.installedProviders = new ArrayList<FileSystemProvider>();

        Set<String> uniqueSchemes = new HashSet<String>();

        // load the remote file system providers
        ServiceLoader<FileSystemProvider> serviceLoader = ServiceLoader.load(FileSystemProvider.class);
        for (FileSystemProvider provider: serviceLoader) {
            if (provider instanceof AbstractRemoteFileSystemProvider) {
                if (uniqueSchemes.add(provider.getScheme())) {
                    this.installedProviders.add(provider);
                } else {
                    throw new IllegalStateException("The remote file system provider type '"+ provider.getClass()+"' with the scheme '"+provider.getScheme()+"' is not unique.");
                }
            }
        }

        // load the default file system providers
        for (FileSystemProvider provider: FileSystemProvider.installedProviders()) {
            if (!(provider instanceof AbstractRemoteFileSystemProvider)) {
                if (uniqueSchemes.add(provider.getScheme())) {
                    this.installedProviders.add(provider);
                } else {
                    throw new IllegalStateException("The default file system provider type '"+ provider.getClass()+"' with the scheme '"+provider.getScheme()+"' is not unique.");
                }
            }
        }
    }
}
