package org.esa.snap.core.dataio;


import org.esa.snap.core.dataio.vfs.remote.object_storage.aws.S3FileSystemProvider;
import org.esa.snap.core.dataio.vfs.remote.object_storage.http.HttpFileSystemProvider;
import org.esa.snap.core.dataio.vfs.remote.object_storage.swift.SwiftFileSystemProvider;

import java.net.URI;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * Paths for VFS
 * Paths custom implementation for providing VFS via Path objects.
 *
 * @author Adrian Drăghici
 */
public class NioPaths {

    private NioPaths() {
    }

    /**
     * Converts a path string, or a sequence of strings that when joined form
     * a path string, to a {@code Path}. If {@code more} does not specify any
     * elements then the value of the {@code first} parameter is the path string
     * to convert. If {@code more} specifies one or more elements then each
     * non-empty string, including {@code first}, is considered to be a sequence
     * of name elements (see {@link Path}) and is joined to form a path string.
     * The details as to how the Strings are joined is provider specific but
     * typically they will be joined using the {@link FileSystem#getSeparator
     * name-separator} as the separator. For example, if the name separator is
     * "{@code /}" and {@code getPath("/foo","bar","gus")} is invoked, then the
     * path string {@code "/foo/bar/gus"} is converted to a {@code Path}.
     * A {@code Path} representing an empty path is returned if {@code first}
     * is the empty string and {@code more} does not contain any non-empty
     * strings.
     *
     * <p> The {@code Path} is obtained by invoking the {@link FileSystem#getPath
     * getPath} method of the {@link FileSystems#getDefault default} {@link
     * FileSystem}.
     *
     * <p> Note that while this method is very convenient, using it will imply
     * an assumed reference to the default {@code FileSystem} and limit the
     * utility of the calling code. Hence it should not be used in library code
     * intended for flexible reuse. A more flexible alternative is to use an
     * existing {@code Path} instance as an anchor, such as:
     * <pre>
     *     Path dir = ...
     *     Path path = dir.resolve("file");
     * </pre>
     *
     * @param first the path string or initial part of the path string
     * @param more  additional strings to be joined to form the path string
     * @return the resulting {@code Path}
     * @throws InvalidPathException if the path string cannot be converted to a {@code Path}
     * @see FileSystem#getPath
     */
    public static Path get(String first, String... more) throws InvalidPathException {
        try {
            for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
                if (provider.getScheme().equals(new SwiftFileSystemProvider().getScheme())) {
                    if (first.startsWith("/" + SwiftFileSystemProvider.SWIFT_ROOT)) {
                        return SwiftFileSystemProvider.getSwiftFileSystem().getPath(first, more);
                    }
                } else if (provider.getScheme().equals(new HttpFileSystemProvider().getScheme())) {
                    if (first.startsWith("/" + HttpFileSystemProvider.HTTP_ROOT)) {
                        return HttpFileSystemProvider.getHttpFileSystem().getPath(first, more);
                    }
                } else if (provider.getScheme().equals(new S3FileSystemProvider().getScheme())) {
                    if (first.startsWith("/" + S3FileSystemProvider.S3_ROOT)) {
                        return S3FileSystemProvider.getS3FileSystem().getPath(first, more);
                    }
                }
            }
        } catch (AccessDeniedException ignored) {
        }
        return FileSystems.getDefault().getPath(first, more);
    }

    /**
     * Converts the given URI to a {@link Path} object.
     *
     * <p> This method iterates over the {@link FileSystemProvider#installedProviders()
     * installed} providers to locate the provider that is identified by the
     * URI {@link URI#getScheme scheme} of the given URI. URI schemes are
     * compared without regard to case. If the provider is found then its {@link
     * FileSystemProvider#getPath getPath} method is invoked to convert the
     * URI.
     *
     * <p> In the case of the default provider, identified by the URI scheme
     * "file", the given URI has a non-empty path component, and undefined query
     * and fragment components. Whether the authority component may be present
     * is platform specific. The returned {@code Path} is associated with the
     * {@link FileSystems#getDefault default} file system.
     *
     * <p> The default provider provides a similar <em>round-trip</em> guarantee
     * to the {@link java.io.File} class. For a given {@code Path} <i>p</i> it
     * is guaranteed that
     * <blockquote><tt>
     * NioPaths.get(</tt><i>p</i><tt>.{@link Path#toUri() toUri}()).equals(</tt>
     * <i>p</i><tt>.{@link Path#toAbsolutePath() toAbsolutePath}())</tt>
     * </blockquote>
     * so long as the original {@code Path}, the {@code URI}, and the new {@code
     * Path} are all created in (possibly different invocations of) the same
     * Java virtual machine. Whether other providers make any guarantees is
     * provider specific and therefore unspecified.
     *
     * @param uri the URI to convert
     * @return the resulting {@code Path}
     * @throws IllegalArgumentException    if preconditions on the {@code uri} parameter do not hold. The
     *                                     format of the URI is provider specific.
     * @throws FileSystemNotFoundException The file system, identified by the URI, does not exist and
     *                                     cannot be created automatically, or the provider identified by
     *                                     the URI's scheme component is not installed
     * @throws SecurityException           if a security manager is installed and it denies an unspecified
     *                                     permission to access the file system
     */
    public static Path get(URI uri) throws IllegalArgumentException, FileSystemNotFoundException, SecurityException {
        String scheme = uri.getScheme();
        if (scheme == null)
            throw new IllegalArgumentException("Missing scheme");

        // check for default provider to avoid loading of installed providers
        if (scheme.equalsIgnoreCase("file"))
            return FileSystems.getDefault().provider().getPath(uri);

        // try to find provider
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (provider.getScheme().equalsIgnoreCase(scheme)) {
                return provider.getPath(uri);
            }
        }

        throw new FileSystemNotFoundException("Provider \"" + scheme + "\" not installed");
    }

    /**
     * Checks if <code>dir</code> is the root of a tree in the virtual file system, such as a HTTP Object Storage VFS.
     * Example: Returns true for "/HTTP:/" on HTTP.
     *
     * @param dir a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>dir</code> is a root of a virtual filesystem
     */
    public static boolean isVirtualFileSystemRoot(java.io.File dir) {
        if (dir instanceof NioFile) {
            for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
                if (provider.getScheme().equals(new SwiftFileSystemProvider().getScheme())) {
                    if (dir.getPath().equals("/" + SwiftFileSystemProvider.SWIFT_ROOT)) {
                        return true;
                    }
                } else if (provider.getScheme().equals(new HttpFileSystemProvider().getScheme())) {
                    if (dir.getPath().equals("/" + HttpFileSystemProvider.HTTP_ROOT)) {
                        return true;
                    }
                } else if (provider.getScheme().equals(new S3FileSystemProvider().getScheme())) {
                    if (dir.getPath().equals("/" + S3FileSystemProvider.S3_ROOT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if <code>path</code> is a VFS path.
     *
     * @param path String representation of a path
     * @return <code>true</code> if <code>path</code> is a VFS path
     */
    public static boolean isVirtualFileSystemPath(String path) {
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (provider.getScheme().equals(new SwiftFileSystemProvider().getScheme())) {
                if (path.startsWith("/" + SwiftFileSystemProvider.SWIFT_ROOT)) {
                    return true;
                }
            } else if (provider.getScheme().equals(new HttpFileSystemProvider().getScheme())) {
                if (path.startsWith("/" + HttpFileSystemProvider.HTTP_ROOT)) {
                    return true;
                }
            } else if (provider.getScheme().equals(new S3FileSystemProvider().getScheme())) {
                if (path.startsWith("/" + S3FileSystemProvider.S3_ROOT)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Provide VFS roots of installed VFS Providers.
     *
     * @return array of {@link NioFile} VFS roots of installed VFS Providers.
     */
    public static NioFile[] getVFSRoots() {
        List<NioFile> roots = new ArrayList<>();
        try {
            for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
                if (provider.getScheme().equals(new SwiftFileSystemProvider().getScheme())) {
                    roots.add(new NioFile(new URI(new SwiftFileSystemProvider().getScheme() + ":/" + SwiftFileSystemProvider.SWIFT_ROOT)));
                } else if (provider.getScheme().equals(new HttpFileSystemProvider().getScheme())) {
                    roots.add(new NioFile(new URI(new HttpFileSystemProvider().getScheme() + ":/" + HttpFileSystemProvider.HTTP_ROOT)));
                } else if (provider.getScheme().equals(new S3FileSystemProvider().getScheme())) {
                    roots.add(new NioFile(new URI(new S3FileSystemProvider().getScheme() + ":/" + S3FileSystemProvider.S3_ROOT)));
                }
            }
            return roots.toArray(new NioFile[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new NioFile[0];
    }

}
