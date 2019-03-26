package org.esa.snap.vfs;


import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * Converts a path string, or a sequence of strings that when joined form a path string, to a {@code Path}. If {@code more} does not specify any elements then the value of the {@code first} parameter is the path string to convert. If {@code more} specifies one or more elements then each non-empty string, including {@code first}, is considered to be a sequence of name elements (see {@link Path}) and is joined to form a path string.
     * The details as to how the Strings are joined is provider specific but typically they will be joined using the {@link FileSystem#getSeparator name-separator} as the separator. For example, if the name separator is
     * "{@code /}" and {@code getPath("/foo","bar","gus")} is invoked, then the path string {@code "/foo/bar/gus"} is converted to a {@code Path}.
     * A {@code Path} representing an empty path is returned if {@code first} is the empty string and {@code more} does not contain any non-empty strings.
     *
     * <p> The {@code Path} is obtained by invoking the {@link FileSystem#getPath getPath} method of the {@link FileSystems#getDefault default} {@link
     * FileSystem}.
     *
     * <p> Note that while this method is very convenient, using it will imply an assumed reference to the default {@code FileSystem} and limit the utility of the calling code. Hence it should not be used in library code intended for flexible reuse. A more flexible alternative is to use an existing {@code Path} instance as an anchor, such as:
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
    public static Path get(String first, String... more) {
        Path path = VFS.getInstance().getVirtualPath(first, more);
        if (path != null) {
            return path;
        }
        return FileSystems.getDefault().getPath(first, more);
    }

    /**
     * Tells whether or not <code>path</code> is a VFS path.
     *
     * @param path String representation of a path
     * @return {@code true} if <code>path</code> is a VFS path
     */
    public static boolean isVirtualFileSystemPath(String path) {
        return VFS.getInstance().getVirtualPath(path) != null;
    }
}
