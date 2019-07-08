package org.esa.snap.vfs.remote;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.LinkPermission;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * File System Service Provider for VFS.
 * Service-provider class for file systems. The methods defined by the {@link java.nio.file.Files} class will typically delegate to an instance of this class.
 *
 * <p> A file system provider is a concrete implementation of this class that implements the abstract methods defined by this class. A provider is identified by a {@code URI} {@link #getScheme() scheme}. The default provider is identified by the URI scheme "file". It creates the {@link FileSystem} that provides access to the file systems accessible to the Java virtual machine.
 * The {@link FileSystems} class defines how file system providers are located and loaded. The default provider is typically a system-default provider but may be overridden if the system property {@code java.nio.file.spi.DefaultFileSystemProvider} is set. In that case, the provider has a one argument constructor whose formal parameter type is {@code
 * FileSystemProvider}. All other providers have a zero argument constructor that initializes the provider.
 *
 * <p> A provider is a factory for one or more {@link FileSystem} instances. Each file system is identified by a {@code URI} where the URI's scheme matches the provider's {@link #getScheme scheme}. The default file system, for example, is identified by the URI {@code "file:///"}. A memory-based file system, for example, may be identified by a URI such as {@code "memory:///?name=logfs"}.
 * The {@link #newFileSystem newFileSystem} method may be used to create a file system, and the {@link #getFileSystem getFileSystem} method may be used to obtain a reference to an existing file system created by the provider. Where a provider is the factory for a single file system then it is provider dependent if the file system is created when the provider is initialized, or later when the {@code newFileSystem} method is invoked. In the case of the default provider, the {@code FileSystem} is created when the provider is initialized.
 *
 * <p> All of the methods in this class are safe for use by multiple concurrent threads.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public abstract class AbstractRemoteFileSystemProvider extends FileSystemProvider implements IRemoteConnectionBuilder {

    private final Map<String, AbstractRemoteFileSystem> fileSystems;

    /**
     * Creates the new File System Service Provider for VFS.
     */
    protected AbstractRemoteFileSystemProvider() {
        this.fileSystems = new HashMap<>();
    }

    private static String extractFileSystemRoot(URI uri) {
        String resourcePathAsString = extractSchemeSpecificPart(uri);
        String substring = ":";
        int index = resourcePathAsString.indexOf(substring);
        if (index < 0) {
            throw new IllegalArgumentException("The scheme specific part '" + resourcePathAsString + "' does not start with the file system root.");
        }
        return resourcePathAsString.substring(0, index + substring.length());
    }

    private static String extractSchemeSpecificPart(URI uri) {
        String resourcePathAsString = uri.getSchemeSpecificPart();
        if (resourcePathAsString.startsWith("/")) {
            resourcePathAsString = resourcePathAsString.substring(1); // remote '/' from the first position
        }
        return resourcePathAsString;
    }

    public abstract void setConnectionData(String fileSystemRoot, String serviceAddress, Map<String, ?> connectionData);

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    protected abstract VFSWalker newObjectStorageWalker(String fileSystemRoot);

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    public abstract String getProviderAddress(String fileSystemRoot);

    public abstract String getProviderFileSeparator(String fileSystemRoot);

    protected abstract AbstractRemoteFileSystem newFileSystem(String fileSystemRoot, Map<String, ?> env);

    /**
     * Constructs a new {@code FileSystem} object identified by a URI. This method is invoked by the {@link FileSystems#newFileSystem(URI, Map)} method to open a new file system identified by a URI.
     * <p> The {@code uri} parameter is an absolute, hierarchical URI, with a scheme equal (without regard to case) to the scheme supported by this provider. The exact form of the URI is highly provider dependent. The
     * {@code env} parameter is a map of provider specific properties to configure the file system.
     * <p> This method throws {@link FileSystemAlreadyExistsException} if the file system already exists because it was previously created by an invocation of this method. Once a file system is {@link
     * FileSystem#close closed} it is provider-dependent if the provider allows a new file system to be created with the same URI as a file system it previously created.
     *
     * @param uri URI reference
     * @param env A map of provider specific properties to configure the file system; may be empty
     * @return A new file system
     * @throws IllegalArgumentException         If the pre-conditions for the {@code uri} parameter aren't met, or the {@code env} parameter does not contain properties required by the provider, or a property value is invalid
     * @throws SecurityException                If a security manager is installed and it denies an unspecified permission required by the file system provider implementation
     * @throws FileSystemAlreadyExistsException If the file system has already been created
     */
    @Override
    public final AbstractRemoteFileSystem newFileSystem(URI uri, Map<String, ?> env) {
        validateScheme(uri);
        String fileSystemRoot = extractFileSystemRoot(uri);
        validateProviderAddress(fileSystemRoot);
        AbstractRemoteFileSystem fileSystem = this.fileSystems.get(fileSystemRoot);
        if (fileSystem == null) {
            fileSystem = newFileSystem(fileSystemRoot, env);
            this.fileSystems.put(fileSystemRoot, fileSystem);
        } else {
            throw new FileSystemAlreadyExistsException(uri.toString());
        }
        return fileSystem;
    }

    /**
     * Returns an existing {@code FileSystem} created by this provider.
     * <p> This method returns a reference to a {@code FileSystem} that was created by invoking the {@link #newFileSystem(URI, Map) newFileSystem(URI,Map)} method. File systems created the {@link #newFileSystem(Path, Map) newFileSystem(Path,Map)} method are not returned by this method.
     * The file system is identified by its {@code URI}. Its exact form is highly provider dependent. In the case of the default provider the URI's path component is {@code "/"} and the authority, query and fragment components are undefined (Undefined components are represented by {@code null}).
     * <p> Once a file system created by this provider is {@link
     * FileSystem#close closed} it is provider-dependent if this method returns a reference to the closed file system or throws {@link
     * FileSystemNotFoundException}. If the provider allows a new file system to be created with the same URI as a file system it previously created then this method throws the exception if invoked after the file system is closed (and before a new instance is created by the {@link #newFileSystem newFileSystem} method).
     * <p> If a security manager is installed then a provider implementation may require to check a permission before returning a reference to an existing file system. In the case of the {@link FileSystems#getDefault default} file system, no permission check is required.
     *
     * @param uri URI reference
     * @return The file system
     * @throws IllegalArgumentException    If the pre-conditions for the {@code uri} parameter aren't met
     * @throws FileSystemNotFoundException If the file system does not exist
     * @throws SecurityException           If a security manager is installed and it denies an unspecified permission.
     */
    @Override
    public final AbstractRemoteFileSystem getFileSystem(URI uri) {
        validateScheme(uri);
        String fileSystemRoot = extractFileSystemRoot(uri);
        validateProviderAddress(fileSystemRoot);
        AbstractRemoteFileSystem fileSystem = this.fileSystems.get(fileSystemRoot);
        if (fileSystem == null) {
            throw new FileSystemNotFoundException(uri.toString());
        }
        return fileSystem;
    }

    /**
     * Returns a {@code Path} object by converting the given {@link URI}. The resulting {@code Path} is associated with a {@link FileSystem} that already exists or is constructed automatically.
     * <p> The exact form of the URI is file system provider dependent. In the case of the default provider, the URI scheme is {@code "file"} and the given URI has a non-empty path component, and undefined query, and fragment components. The resulting {@code Path} is associated with the default {@link FileSystems#getDefault default} {@code FileSystem}.
     * <p> If a security manager is installed then a provider implementation may require to check a permission. In the case of the {@link
     * FileSystems#getDefault default} file system, no permission check is required.
     *
     * @param uri The URI to convert
     * @return The resulting {@code Path}
     * @throws IllegalArgumentException    If the URI scheme does not identify this provider or other preconditions on the uri parameter do not hold
     * @throws FileSystemNotFoundException The file system, identified by the URI, does not exist and cannot be created automatically
     * @throws SecurityException           If a security manager is installed and it denies an unspecified permission.
     */
    @Override
    public final Path getPath(URI uri) {
        validateScheme(uri);
        String fileSystemRoot = extractFileSystemRoot(uri);
        validateProviderAddress(fileSystemRoot);
        AbstractRemoteFileSystem fileSystem = this.fileSystems.computeIfAbsent(fileSystemRoot, fsr -> newFileSystem(fsr, Collections.emptyMap()));
        String resourcePathAsString = extractSchemeSpecificPart(uri);
        return VFSPath.parsePath(fileSystem, resourcePathAsString);
    }

    /**
     * Opens or creates a file for reading and/or writing, returning a file channel to access the file. This method works in exactly the manner specified by the {@link FileChannel#open(Path, Set, FileAttribute[])
     * FileChannel.open} method. A provider that does not support all the features required to construct a file channel throws {@code
     * UnsupportedOperationException}. The default provider is required to support the creation of file channels. When not overridden, the default implementation throws {@code UnsupportedOperationException}.
     *
     * @param path    the path of the file to open or create
     * @param options options specifying how the file is opened
     * @param attrs   an optional list of file attributes to set atomically when creating the file
     * @return a new file channel
     * @throws IllegalArgumentException      If the set contains an invalid combination of options
     * @throws UnsupportedOperationException If this provider that does not support creating file channels, or an unsupported open option or file attribute is specified
     * @throws IOException                   If an I/O error occurs
     * @throws SecurityException             In the case of the default file system, the {@link
     *                                       SecurityManager#checkRead(String)} method is invoked to check read access if the file is opened for reading. The {@link
     *                                       SecurityManager#checkWrite(String)} method is invoked to check write access if the file is opened for writing
     */
    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        VFSPath remotePath = VFSPath.toRemotePath(path);
        return new VFSFileChannel(remotePath, options, attrs);
    }

    /**
     * Opens or creates a file, returning a seekable byte channel to access the file. This method works in exactly the manner specified by the {@link
     * Files#newByteChannel(Path, Set, FileAttribute[])} method.
     *
     * @param path    the path to the file to open or create
     * @param options options specifying how the file is opened
     * @param attrs   an optional list of file attributes to set atomically when creating the file
     * @return a new seekable byte channel
     * @throws IllegalArgumentException      if the set contains an invalid combination of options
     * @throws UnsupportedOperationException if an unsupported open option is specified or the array contains attributes that cannot be set atomically when creating the file
     * @throws FileAlreadyExistsException    if a file of that name already exists and the {@link
     *                                       StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *                                       <i>(optional specific exception)</i>
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager is installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the path if the file is opened for reading. The {@link SecurityManager#checkWrite(String) checkWrite} method is invoked to check write access to the path if the file is opened for writing. The {@link
     *                                       SecurityManager#checkDelete(String) checkDelete} method is invoked to check delete access if the file is opened with the
     *                                       {@code DELETE_ON_CLOSE} option.
     */
    @Override
    public VFSByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>[] attrs) throws IOException {
        VFSPath remotePath = VFSPath.toRemotePath(path);
        AbstractRemoteFileSystem fileSystem = remotePath.getFileSystem();
        return fileSystem.openByteChannel(remotePath, options, attrs);
    }

    /**
     * Opens a directory, returning a {@code DirectoryStream} to iterate over the entries in the directory. This method works in exactly the manner specified by the {@link Files#newDirectoryStream(Path, DirectoryStream.Filter)} method.
     *
     * @param dir    the path to the directory
     * @param filter the directory stream filter
     * @return a new and open {@code DirectoryStream} object
     * @throws NotDirectoryException if the file could not otherwise be opened because it is not a directory <i>(optional specific exception)</i>
     * @throws IOException           if an I/O error occurs
     * @throws SecurityException     In the case of the default provider, and a security manager is installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the directory.
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        VFSPath remoteDir = VFSPath.toRemotePath(dir);
        AbstractRemoteFileSystem fileSystem = remoteDir.getFileSystem();
        Iterable<Path> directories = fileSystem.walkDir(remoteDir, filter);
        return new DirectoryStream<Path>() {
            @Override
            public Iterator<Path> iterator() {
                return directories.iterator();
            }

            @Override
            public void close() {
                //stay open
            }
        };
    }

    /**
     * Creates a new directory. This method works in exactly the manner specified by the {@link Files#createDirectory} method.
     *
     * @param dir   the directory to create
     * @param attrs an optional list of file attributes to set atomically when creating the directory
     * @throws UnsupportedOperationException if the array contains an attribute that cannot be set atomically when creating the directory
     * @throws SecurityException             In the case of the default provider, and a security manager is installed, the {@link SecurityManager#checkWrite(String) checkWrite} method is invoked to check write access to the new directory.
     */
    @Override
    public void createDirectory(Path dir, FileAttribute<?>[] attrs) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a file. This method works in exactly the  manner specified by the
     * {@link Files#delete} method.
     *
     * @param path the path to the file to delete
     * @throws SecurityException In the case of the default provider, and a security manager is installed, the {@link SecurityManager#checkDelete(String)} method is invoked to check delete access to the file
     */
    @Override
    public void delete(Path path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Copy a file to a target file. This method works in exactly the manner specified by the {@link Files#copy(Path, Path, CopyOption[])} method except that both the source and target paths must be associated with this provider.
     *
     * @param source  the path to the file to copy
     * @param target  the path to the target file
     * @param options options specifying how the copy should be done
     * @throws SecurityException In the case of the default provider, and a security manager is installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the source file, the
     *                           {@link SecurityManager#checkWrite(String) checkWrite} is invoked to check write access to the target file. If a symbolic link is copied the security manager is invoked to check {@link
     *                           LinkPermission}{@code ("symbolic")}.
     */
    @Override
    public void copy(Path source, Path target, CopyOption... options) {
        throw new UnsupportedOperationException();
    }

    /**
     * Move or rename a file to a target file. This method works in exactly the manner specified by the {@link Files#move} method except that both the source and target paths must be associated with this provider.
     *
     * @param source  the path to the file to move
     * @param target  the path to the target file
     * @param options options specifying how the move should be done
     * @throws SecurityException In the case of the default provider, and a security manager is installed, the {@link SecurityManager#checkWrite(String) checkWrite} method is invoked to check write access to both the source and target file.
     */
    @Override
    public void move(Path source, Path target, CopyOption... options) {
        throw new UnsupportedOperationException();
    }

    /**
     * Tests if two paths locate the same file. This method works in exactly the manner specified by the {@link Files#isSameFile} method.
     *
     * @param path1  one path to the file
     * @param path2 the other path
     * @return {@code true} if, and only if, the two paths locate the same file
     * @throws SecurityException In the case of the default provider, and a security manager is installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to both files.
     */
    @Override
    public boolean isSameFile(Path path1, Path path2) {
        VFSPath remotePath1 = VFSPath.toRemotePath(path1);
        VFSPath remotePath2 = VFSPath.toRemotePath(path2);
        return remotePath1.equals(remotePath2);
    }

    /**
     * Tells whether or not a file is considered <em>hidden</em>. This method works in exactly the manner specified by the {@link Files#isHidden} method.
     * <p> This method is invoked by the {@link Files#isHidden isHidden} method.
     *
     * @param path the path to the file to test
     * @return {@code true} if the file is considered hidden
     * @throws SecurityException In the case of the default provider, and a security manager is installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the file.
     */
    @Override
    public boolean isHidden(Path path) {
        VFSPath.toRemotePath(path); // check the path type
        return false;
    }

    /**
     * Returns the {@link FileStore} representing the file store where a file is located. This method works in exactly the manner specified by the
     * {@link Files#getFileStore} method.
     *
     * @param path the path to the file
     * @return the file store where the file is stored
     * @throws SecurityException In the case of the default provider, and a security manager is installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the file, and in addition it checks {@link RuntimePermission}<tt>
     *                           ("getFileStoreAttributes")</tt>
     */
    @Override
    public FileStore getFileStore(Path path) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks the existence, and optionally the accessibility, of a file.
     *
     * @param path  the path to the file to check
     * @param accessModes The access modes to check; may have zero elements
     * @throws UnsupportedOperationException an implementation is required to support checking for
     *                                       {@code READ}, {@code WRITE}, and {@code EXECUTE} access. This exception is specified to allow for the {@code Access} enum to be extended in future releases.               if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, and a security manager is installed, the {@link SecurityManager#checkRead(String) checkRead} is invoked when checking read access to the file or only the existence of the file, the {@link SecurityManager#checkWrite(String) checkWrite} is invoked when checking write access to the file, and {@link SecurityManager#checkExec(String) checkExec} is invoked when checking execute access.
     */
    @Override
    public void checkAccess(Path path, AccessMode... accessModes) throws IOException {
        VFSPath remotePath = VFSPath.toRemotePath(path);
        if (accessModes.length == 0) {
            checkRead(remotePath);
        } else {
            for (AccessMode mode: accessModes) {
                if (mode == AccessMode.READ) {
                    checkRead(remotePath);
                } else {
                    throw new IOException("The access mode '" + mode + "' is not allowed.");
                }
            }
        }
    }

    /**
     * Returns a file attribute view of a given type. This method works in exactly the manner specified by the {@link Files#getFileAttributeView} method.
     *
     * @param path    the path to the file
     * @param type    the {@code Class} object corresponding to the file attribute view
     * @param linkOptions options indicating how symbolic links are handled
     * @return a file attribute view of the specified type, or {@code null} if the attribute view type is not available
     */
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... linkOptions) {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads a file's attributes as a bulk operation. This method works in exactly the manner specified by the {@link
     * Files#readAttributes(Path, Class, LinkOption[])} method.
     *
     * @param path    the path to the file
     * @param type    the {@code Class} of the file attributes required to read
     * @param linkOptions options indicating how symbolic links are handled
     * @return the file attributes
     * @throws UnsupportedOperationException if an attributes of the given type are not supported
     * @throws IOException                   if an I/O error occurs
     * @throws SecurityException             In the case of the default provider, a security manager is installed, its {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the file
     */
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... linkOptions) throws IOException {
        if (linkOptions.length == 0 || (linkOptions.length == 1 && linkOptions[0] == LinkOption.NOFOLLOW_LINKS)) {
            VFSPath remotePath = VFSPath.toRemotePath(path);
            BasicFileAttributes fileAttributes = checkRead(remotePath);
            return type.cast(fileAttributes);
        } else {
            throw new IOException("The link options must be empty or only '" + LinkOption.NOFOLLOW_LINKS + "' is allowed.");
        }
    }

    /**
     * Reads a set of file attributes as a bulk operation. This method works in exactly the manner specified by the {@link
     * Files#readAttributes(Path, String, LinkOption[])} method.
     *
     * @param path       the path to the file
     * @param attributes the attributes to read
     * @param options    options indicating how symbolic links are handled
     * @return a map of the attributes returned; may be empty. The map's keys are the attribute names, its values are the attribute values
     * @throws UnsupportedOperationException if the attribute view is not available
     * @throws IllegalArgumentException      if no attributes are specified or an unrecognized attributes is specified
     * @throws SecurityException             In the case of the default provider, and a security manager is installed, its {@link SecurityManager#checkRead(String) checkRead} method denies read access to the file. If this method is invoked to read security sensitive attributes then the security manager may be invoke to check for additional permissions.
     */
    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the value of a file attribute. This method works in exactly the manner specified by the {@link Files#setAttribute} method.
     *
     * @param path      the path to the file
     * @param attribute the attribute to set
     * @param value     the attribute value
     * @param options   options indicating how symbolic links are handled
     * @throws UnsupportedOperationException if the attribute view is not available
     * @throws IllegalArgumentException      if the attribute name is not specified, or is not recognized, or the attribute value is of the correct type but has an inappropriate value
     * @throws ClassCastException            If the attribute value is not of the expected type or is a collection containing elements that are not of the expected type
     * @throws SecurityException             In the case of the default provider, and a security manager is installed, its {@link SecurityManager#checkWrite(String) checkWrite} method denies write access to the file. If this method is invoked to set security sensitive attributes then the security manager may be invoked to check for additional permissions.
     */
    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) {
        throw new UnsupportedOperationException();
    }

    public Path getPathIfFileSystemRootMatches(String first, String... more) {
        for (Map.Entry<String, AbstractRemoteFileSystem> entry : this.fileSystems.entrySet()) {
            String fileSystemRoot = entry.getKey();
            if (first.startsWith(fileSystemRoot)) {
                AbstractRemoteFileSystem remoteFileSystem = entry.getValue();
                return remoteFileSystem.getPath(first, more);
            }
        }
        return null;
    }

    public final AbstractRemoteFileSystem getFileSystemOrCreate(URI uri, Map<String, ?> env) {
        validateScheme(uri);
        String fileSystemRoot = extractFileSystemRoot(uri);
        validateProviderAddress(fileSystemRoot);
        return this.fileSystems.computeIfAbsent(fileSystemRoot, fsr -> newFileSystem(fsr, env));
    }

    /**
     * Removes the FS from VFS list of this root provider.
     *
     * @param fileSystem The FS to remove
     */
    void unlinkFileSystem(AbstractRemoteFileSystem fileSystem) {
        for (Map.Entry<String, AbstractRemoteFileSystem> entry : this.fileSystems.entrySet()) {
            if (entry.getValue() == fileSystem) {
                this.fileSystems.remove(entry.getKey());
                return;
            }
        }
    }

    private void validateScheme(URI uri) {
        if (!getScheme().equals(uri.getScheme())) {
            throw new IllegalArgumentException("The provider scheme '" + getScheme() + "' is not the same with the uri scheme '" + uri.getScheme() + "'.");
        }
    }

    private void validateProviderAddress(String fileSystemRoot) {
        if (getProviderAddress(fileSystemRoot).isEmpty()) {
            throw new IllegalStateException("The VFS with scheme: " + getScheme() + " is not initialized.");
        }
    }

    private static BasicFileAttributes checkRead(VFSPath path) throws IOException {
        BasicFileAttributes fileAttributes = path.getFileAttributes();
        if (fileAttributes == null) {
            AbstractRemoteFileSystem fileSystem = path.getFileSystem();
            fileAttributes = fileSystem.newObjectStorageWalker().readBasicFileAttributes(path);
            path.setFileAttributes(fileAttributes);
        }
        return fileAttributes;
    }
}
