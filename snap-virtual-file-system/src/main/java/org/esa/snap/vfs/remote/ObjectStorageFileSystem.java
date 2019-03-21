package org.esa.snap.vfs.remote;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * File System for Object Storage VFS.
 * Provides an interface to a file system and is the factory for objects to access files and other objects in the file system.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public abstract class ObjectStorageFileSystem extends FileSystem {

    private static Logger logger = Logger.getLogger(ObjectStorageFileSystem.class.getName());

    private final AbstractRemoteFileSystemProvider provider;
    private final String address;
    private final ObjectStoragePath root;
    private final ObjectStoragePath empty;
    private String separator;
    private boolean closed;
    private List<ObjectStorageByteChannel> openChannels;
    private ObjectStorageWalker walker;

    /**
     * Creates the new File System for Object Storage VFS.
     *
     * @param provider  The VFS provider
     * @param address   The VFS service address
     * @param separator The VFS path separator
     */
    protected ObjectStorageFileSystem(AbstractRemoteFileSystemProvider provider, String address, String separator) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        if (address == null) {
            throw new NullPointerException("address");
        }
        if (address.isEmpty()) {
            throw new IllegalArgumentException("address is empty");
        }
        if (separator.isEmpty()) {
            throw new IllegalArgumentException("separator is empty");
        }
        this.provider = provider;
        this.address = address;
        this.separator = separator;
        this.closed = false;
        this.openChannels = new ArrayList<>();
        this.root = new ObjectStoragePath(this, true, true, "" + provider.getRoot(), ObjectStorageFileAttributes.getRoot());
        this.empty = new ObjectStoragePath(this, false, false, "", ObjectStorageFileAttributes.getEmpty());
    }

    /**
     * Returns the VFS provider that created this file system.
     *
     * @return The VFS provider that created this file system.
     */
    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    /**
     * Gets the VFS service address URL as string.
     *
     * @return The VFS service address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Gets the VFS root path.
     *
     * @return The VFS root path
     */
    ObjectStoragePath getRoot() {
        return root;
    }

    /**
     * Gets the VFS empty path.
     *
     * @return The VFS empty path
     */
    ObjectStoragePath getEmpty() {
        return empty;
    }

    /**
     * Closes this VFS.
     *
     * @throws IOException                   If an I/O error occurs
     * @throws UnsupportedOperationException Thrown in the case of the default file system
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        Channel[] channels = openChannels.toArray(new Channel[0]);
        for (Channel channel : channels) {
            channel.close();
        }
        provider.unlinkFileSystem(this);
    }

    /**
     * Tells whether or not this file system is open.
     *
     * @return {@code true} if, and only if, this file system is open
     */
    @Override
    public boolean isOpen() {
        return !closed;
    }

    /**
     * Tells whether or not this file system allows only read-only access to
     * its file stores.
     *
     * @return {@code true} if, and only if, this file system provides
     * read-only access
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Returns the name separator, represented as a string.
     *
     * @return The name separator
     */
    @Override
    public String getSeparator() {
        return separator;
    }

    /**
     * Returns an object to iterate over the paths of the root directories.
     *
     * @return An object to iterate over the root directories
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        return walkDir(getRoot(), path -> ((ObjectStoragePath) path).isDirectory());
    }

    /**
     * Returns an object to iterate over the underlying file stores.
     *
     * @return An object to iterate over the backing file stores
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the set of the {@link FileAttributeView#name names} of the file
     * attribute views supported by this {@code FileSystem}.
     *
     * @return An unmodifiable set of the names of the supported file attribute
     * views
     */
    @Override
    public Set<String> supportedFileAttributeViews() {
        throw new UnsupportedOperationException();
    }

    /**
     * Converts a path string, or a sequence of strings that when joined form
     * a path string, to a {@code Path}. If {@code more} does not specify any
     * elements then the value of the {@code first} parameter is the path string
     * to convert. If {@code more} specifies one or more elements then each
     * non-empty string, including {@code first}, is considered to be a sequence
     * of name elements (see {@link Path}) and is joined to form a path string.
     * The details as to how the Strings are joined is provider specific but
     * typically they will be joined using the {@link #getSeparator
     * name-separator} as the separator. For example, if the name separator is
     * "{@code /}" and {@code getPath("/foo","bar","gus")} is invoked, then the
     * path string {@code "/foo/bar/gus"} is converted to a {@code Path}.
     * A {@code Path} representing an empty path is returned if {@code first}
     * is the empty string and {@code more} does not contain any non-empty
     * strings.
     *
     * @param first the path string or initial part of the path string
     * @param more  additional strings to be joined to form the path string
     * @return the resulting {@code Path}
     * @throws InvalidPathException If the path string cannot be converted
     */
    @Override
    @NotNull
    public Path getPath(String first, String... more) {
        assertOpen();
        String pathName = first;
        if (more.length > 0) {
            String pathSeparator = getSeparator();
            pathName += pathSeparator + String.join(pathSeparator, more);
        }
        return ObjectStoragePath.parsePath(this, pathName);
    }

    /**
     * Returns a {@code PathMatcher} that performs match operations on the
     * {@code String} representation of {@link Path} objects by interpreting a
     * given pattern.
     *
     * @param syntaxAndPattern The syntax and pattern
     * @return A path matcher that may be used to match paths against the pattern
     * @throws IllegalArgumentException      If the parameter does not take the form: {@code syntax:pattern}
     * @throws PatternSyntaxException        If the pattern is invalid
     * @throws UnsupportedOperationException If the pattern syntax is not known to the implementation
     * @see Files#newDirectoryStream(Path, String)
     */
    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the {@code UserPrincipalLookupService} for this file system
     * <i>(optional operation)</i>. The resulting lookup service may be used to
     * lookup user or group names.
     *
     * @return The {@code UserPrincipalLookupService} for this file system
     * @throws UnsupportedOperationException If this {@code FileSystem} does not does have a lookup service
     */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    /**
     * Constructs a new {@link WatchService} <i>(optional operation)</i>.
     *
     * @return a new watch service
     * @throws UnsupportedOperationException If this {@code FileSystem} does not support watching file system
     *                                       objects for changes and events. This exception is not thrown
     *                                       by {@code FileSystems} created by the default provider.
     * @throws IOException                   If an I/O error occurs
     */
    @Override
    public WatchService newWatchService() throws IOException {
        throw new IOException(new UnsupportedOperationException());
    }

    private void assertOpen() {
        if (closed) {
            throw new ClosedFileSystemException();
        }
    }

    /**
     * Opens a Byte Channel for given VFS path.
     *
     * @param path    The VFS path
     * @param options The open options
     * @param attrs   The attributes
     * @return The Byte Channel
     * @throws IOException If an I/O error occurs
     */
    SeekableByteChannel openByteChannel(ObjectStoragePath path, Set<? extends OpenOption> options, FileAttribute<?>[] attrs) throws IOException {
        boolean plainReadWriteMode = options.isEmpty() || options.size() == 1 && (options.contains(StandardOpenOption.READ) || options.contains(StandardOpenOption.WRITE));
        boolean noCreateAttributes = attrs.length == 0;
        if (plainReadWriteMode && noCreateAttributes) {
            return addByteChannel(new ObjectStorageByteChannel(path));
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a new Byte Channel to list of opened Byte Channels
     *
     * @param channel The Byte Channel
     * @return The Byte Channel
     */
    private ObjectStorageByteChannel addByteChannel(ObjectStorageByteChannel channel) {
        openChannels.add(channel);
        return channel;
    }

    /**
     * Removes a new Byte Channel from list of opened Byte Channels
     *
     * @param channel The Byte Channel
     */
    void removeByteChannel(ObjectStorageByteChannel channel) {
        openChannels.remove(channel);
    }

    /**
     * Browse a given VFS directory path.
     *
     * @param dir    The VFS directory path
     * @param filter The filter for results
     * @return The browsing results
     */
    Iterable<Path> walkDir(Path dir, DirectoryStream.Filter<? super Path> filter) {
        assertOpen();
        Path path = dir.toAbsolutePath();
        String prefix = path.toString().substring(1);
        List<BasicFileAttributes> files;
        if (walker == null) {
            walker = provider.newObjectStorageWalker();
        }
        try {
            files = walker.walk(prefix);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to browse the path: " + dir.toString() + ". Details: " + ex.getMessage());
            throw new IllegalStateException(ex);
        }
        return files.stream()
                .map(f -> ObjectStoragePath.fromFileAttributes(this, f))
                .filter(p -> filterPath(p, filter))
                .collect(Collectors.toList());
    }

    /**
     * Tells whether or not <code>path</code> is accepted by a filter.
     *
     * @param path   The VFS path
     * @param filter The filter
     * @return {@code true} if <code>path</code> is accepted by a filter
     */
    private boolean filterPath(ObjectStoragePath path, DirectoryStream.Filter<? super Path> filter) {
        try {
            return filter.accept(path);
        } catch (IOException ex) {
            logger.log(Level.FINE, "Unable to filter the path: " + path.toString() + ". Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Returns a reference to an existing {@code FileSystem}.
     *
     * <p> This method iterates over the {@link AbstractRemoteFileSystemProvider#installedProviders().installed} providers to locate the provider that is identified by the URI {@link URI#getScheme scheme} of the given URI. URI schemes are compared without regard to case. The exact form of the URI is highly provider dependent. If found, the provider's {@link FileSystemProvider#getFileSystem getFileSystem} method is invoked to obtain a reference to the {@code FileSystem}.
     *
     * @param   uri  the URI to locate the file system
     *
     * @return  the reference to the file system
     *
     * @throws  IllegalArgumentException if the pre-conditions for the {@code uri} parameter are not met
     * @throws java.nio.file.FileSystemNotFoundException if the file system, identified by the URI, does not exist
     * @throws  ProviderNotFoundException if a provider supporting the URI scheme is not installed
     * @throws  SecurityException if a security manager is installed and it denies an unspecified permission
     */
    public static FileSystem getFileSystem(URI uri) {
        String scheme = uri.getScheme();
        for (FileSystemProvider provider : AbstractRemoteFileSystemProvider.installedProviders()) {
            if (scheme.equalsIgnoreCase(provider.getScheme())) {
                return provider.getFileSystem(uri);
            }
        }
        throw new ProviderNotFoundException("Provider \"" + scheme + "\" not found");
    }

    /**
     * Constructs a new file system that is identified by a {@link URI}
     *
     * <p> This method iterates over the {@link AbstractRemoteFileSystemProvider#installedProviders() installed} providers to locate the provider that is identified by the URI {@link URI#getScheme scheme} of the given URI. URI schemes are compared without regard to case. The exact form of the URI is highly provider dependent. If found, the provider's {@link FileSystemProvider#newFileSystem(URI, java.util.Map).newFileSystem(URI,Map)} method is invoked to construct the new file system.
     *
     * <p> Once a file system is {@link FileSystem#close closed} it is provider-dependent if the provider allows a new file system to be created with the same URI as a file system it previously created.
     *
     * <p> <b>Usage Example:</b>
     * Suppose there is a provider identified by the scheme {@code "memory"} installed:
     * <pre>
     *   Map&lt;String,String&gt; env = new HashMap&lt;&gt;();
     *   env.put("capacity", "16G");
     *   env.put("blockSize", "4k");
     *   FileSystem fs = FileSystems.newFileSystem(URI.create("memory:///?name=logfs"), env);
     * </pre>
     *
     * @param   uri the URI identifying the file system
     * @param   env a map of provider specific properties to configure the file system; may be empty
     *
     * @return  a new file system
     *
     * @throws  IllegalArgumentException if the pre-conditions for the {@code uri} parameter are not met, or the {@code env} parameter does not contain properties required by the provider, or a property value is invalid
     * @throws java.nio.file.FileSystemAlreadyExistsException if the file system has already been created
     * @throws  ProviderNotFoundException if a provider supporting the URI scheme is not installed
     * @throws  IOException if an I/O error occurs creating the file system
     * @throws  SecurityException if a security manager is installed and it denies an unspecified permission required by the file system provider implementation
     */
    public static FileSystem newFileSystem(URI uri, Map<String,?> env) throws IOException
    {
        String scheme = uri.getScheme();
        for (FileSystemProvider provider: AbstractRemoteFileSystemProvider.installedProviders()) {
            if (scheme.equalsIgnoreCase(provider.getScheme())) {
                return provider.newFileSystem(uri, env);
            }
        }
        throw new ProviderNotFoundException("Provider \"" + scheme + "\" not found");
    }


}
