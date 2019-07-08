package org.esa.snap.vfs;

import org.esa.snap.vfs.preferences.model.Property;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

import java.net.URI;
import java.net.URISyntaxException;
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
 * VFS Core class
 *
 * @author Jean Coravu
 * @author Adrian Drăghici
 */
public class VFS {

    private static final VFS instance;

    static {
        instance = new VFS();
        instance.loadInstalledProviders();
    }

    private List<FileSystemProvider> installedProviders;

    private VFS() {
    }

    /**
     * Gets the VFS instance for provide access to VFS core methods.
     *
     * @return the new VFS instance
     */
    public static VFS getInstance() {
        return instance;
    }

    /**
     * Gets the list with VFS Providers and installed OS FS Providers.
     *
     * @return the list with FS Providers
     */
    private List<FileSystemProvider> getInstalledProviders() {
        return this.installedProviders;
    }

    /**
     * Gets the FS provider identified by scheme.
     *
     * @param scheme the FS provider scheme
     * @return the FS provider
     */
    public FileSystemProvider getFileSystemProviderByScheme(String scheme) {
        for (FileSystemProvider fileSystemProvider : this.installedProviders) {
            if (scheme.equalsIgnoreCase(fileSystemProvider.getScheme())) {
                return fileSystemProvider;
            }
        }
        return null;
    }

    /**
     * Initializes the installed Remote Virtual File System Providers using Remote File Repositories Configurations.
     *
     * @param vfsRepositories the Remote File Repositories Configurations
     */
    public void initRemoteInstalledProviders(List<VFSRemoteFileRepository> vfsRepositories) {
        for (FileSystemProvider fileSystemProvider : this.installedProviders) {
            if (!(fileSystemProvider instanceof AbstractRemoteFileSystemProvider)) {
                continue;
            }
            AbstractRemoteFileSystemProvider remoteFileSystemProvider = (AbstractRemoteFileSystemProvider) fileSystemProvider;

            for (VFSRemoteFileRepository repository : vfsRepositories) {
                if (!repository.getScheme().equalsIgnoreCase(remoteFileSystemProvider.getScheme())) {
                    continue;
                }
                Map<String, String> connectionData = new HashMap<>();
                for (Property vfsRemoteFileRepositoryProperty : repository.getProperties()) {
                    connectionData.put(vfsRemoteFileRepositoryProperty.getName(), vfsRemoteFileRepositoryProperty.getValue());
                }
                String fileSystemRoot = repository.getRoot();
                remoteFileSystemProvider.setConnectionData(fileSystemRoot, repository.getAddress(), connectionData);
                try {
                    remoteFileSystemProvider.getFileSystemOrCreate(new URI(repository.getScheme(), fileSystemRoot, null), null);
                } catch (URISyntaxException e) {
                    throw new ExceptionInInitializerError("Unable to initialize VFS with scheme: " + remoteFileSystemProvider.getScheme());
                }
            }
        }
    }

    /**
     * Converts the given URI to a {@link Path} object.
     *
     * @param uri the URI to convert
     * @return the resulting {@code Path}
     */
    public Path getPath(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("Missing scheme.");
        }
        // check for default provider to avoid loading of installed providers
        if (scheme.equalsIgnoreCase("file")) {
            return FileSystems.getDefault().provider().getPath(uri);
        }
        for (FileSystemProvider provider : this.installedProviders) {
            if (provider.getScheme().equalsIgnoreCase(scheme)) {
                return provider.getPath(uri);
            }
        }
        throw new FileSystemNotFoundException("The file system provider with the scheme '" + scheme + "' is not installed.");
    }

    /**
     * Converts a path string, or a sequence of strings that when joined form a path string, to a {@code Path}.
     *
     * @param first the path string or initial part of the path string
     * @param more  additional strings to be joined to form the path string
     * @return the resulting {@code Path}
     */
    public Path get(String first, String... more) {
        Path path = getVirtualPath(first, more);
        if (path != null) {
            return path;
        }
        return FileSystems.getDefault().getPath(first, more);
    }

    /**
     * Converts a path string, or a sequence of strings that when joined form a path string, to a {@code VFSPath}.
     *
     * @param first the path string or initial part of the path string
     * @param more  additional strings to be joined to form the path string
     * @return the resulting {@code VFSPath}
     */
    private Path getVirtualPath(String first, String... more) {
        for (FileSystemProvider provider : VFS.getInstance().getInstalledProviders()) {
            if (provider instanceof AbstractRemoteFileSystemProvider) {
                AbstractRemoteFileSystemProvider remoteFileSystemProvider = (AbstractRemoteFileSystemProvider) provider;
                Path path = remoteFileSystemProvider.getPathIfFileSystemRootMatches(first, more);
                if (path != null) {
                    return path;
                }
            }
        }
        return null;
    }

    /**
     * Loads the installed FS Providers (VFS and OS Providers)
     */
    private void loadInstalledProviders() {
        this.installedProviders = new ArrayList<>();

        Set<String> uniqueSchemes = new HashSet<>();

        // load the remote file system providers
        ServiceLoader<FileSystemProvider> serviceLoader = ServiceLoader.load(FileSystemProvider.class);
        for (FileSystemProvider provider : serviceLoader) {
            if (provider instanceof AbstractRemoteFileSystemProvider) {
                if (uniqueSchemes.add(provider.getScheme())) {
                    this.installedProviders.add(provider);
                } else {
                    throw new IllegalStateException("The remote file system provider type '" + provider.getClass() + "' with the scheme '" + provider.getScheme() + "' is not unique.");
                }
            }
        }

        // load the default file system providers
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (!(provider instanceof AbstractRemoteFileSystemProvider)) {
                if (uniqueSchemes.add(provider.getScheme())) {
                    this.installedProviders.add(provider);
                } else {
                    throw new IllegalStateException("The default file system provider type '" + provider.getClass() + "' with the scheme '" + provider.getScheme() + "' is not unique.");
                }
            }
        }
    }
}
