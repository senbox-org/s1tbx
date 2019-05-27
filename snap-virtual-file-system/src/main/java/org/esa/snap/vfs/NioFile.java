package org.esa.snap.vfs;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File for VFS.
 * File class custom implementation for providing VFS via File objects.
 *
 * @author Adrian Drăghici
 */
public class NioFile extends File {

    private static final String INVALID_FILE_PATH_ERROR_MESSAGE = "Invalid file path.";

    private static Logger logger = Logger.getLogger(NioFile.class.getName());

    /**
     * The Path object representing the file system path.
     */
    private final Path path;
    /**
     * The flag indicating whether the file path is invalid.
     */
    private transient NioFile.PathStatus status;

    /**
     * Creates a new File for VFS using the given path
     *
     * @param path The VFS path
     */
    public NioFile(Path path) {
        super(path.toString());

        this.path = path;
    }


    /* -- Constructors -- */

    /**
     * Creates a new File for VFS using the given path name
     *
     * @param pathname The VFS path name
     */
    public NioFile(String pathname) {
        this(NioPaths.get(pathname));
    }

    private static String slashify(Path path, boolean isDirectory) {
        String p = path.toString();
        if (!path.getFileSystem().getSeparator().equals("/"))
            p = p.replace(path.getFileSystem().getSeparator(), "/");
        if (!p.startsWith("/"))
            p = "/" + p;
        if (!p.endsWith("/") && isDirectory)
            p = p + "/";
        return p;
    }

    /* -- Path-component accessors -- */

    /**
     * Check if the file has an invalid path. Currently, the inspection of a file path is very limited, and it only covers Nul character check.
     * Returning true means the path is definitely invalid/garbage. But returning false does not guarantee that the path is valid.
     *
     * @return true if the file path is invalid.
     */
    private boolean isInvalidPath() {
        if (status == null) {
            status = (this.path.toString().indexOf('\u0000') < 0) ? NioFile.PathStatus.CHECKED : NioFile.PathStatus.INVALID;
        }
        return status == NioFile.PathStatus.INVALID;
    }

    /**
     * Returns the name of the file or directory denoted by this abstract pathname.  This is just the last name in the pathname's name sequence.  If the pathname's name sequence is empty, then the empty string is returned.
     *
     * @return The name of the file or directory denoted by this abstract pathname, or the empty string if this pathname's name sequence is empty
     */
    @Override
    public String getName() {
        Object fileName = path.getFileName();
        String name = "";
        if (fileName != null) {
            name = fileName.toString();
            if (name.endsWith(this.path.getFileSystem().getSeparator())) {
                name = name.substring(0, name.length() - 1);
            }
        }
        return name;
    }

    /**
     * Returns the pathname string of this abstract pathname's parent, or
     * <code>null</code> if this pathname does not name a parent directory.
     *
     * <p> The <em>parent</em> of an abstract pathname consists of the pathname's prefix, if any, and each name in the pathname's name sequence except for the last.  If the name sequence is empty then the pathname does not name a parent directory.
     *
     * @return The pathname string of the parent directory named by this abstract pathname, or <code>null</code> if this pathname does not name a parent
     */
    @Override
    public String getParent() {
        Path parent = path.getParent();
        return parent != null ? parent.toString() : null;
    }


    /* -- Path operations -- */

    /**
     * Returns the abstract pathname of this abstract pathname's parent, or <code>null</code> if this pathname does not name a parent directory.
     *
     * <p> The <em>parent</em> of an abstract pathname consists of the pathname's prefix, if any, and each name in the pathname's name sequence except for the last.  If the name sequence is empty then the pathname does not name a parent directory.
     *
     * @return The abstract pathname of the parent directory named by this abstract pathname, or <code>null</code> if this pathname does not name a parent
     * @since 1.2
     */
    @Override
    public File getParentFile() {
        Path p = path.getParent();
        if (p != null)
            try {
                return new NioFile(p);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unable to get the parent file path of path: " + path.toString() + ". Details: " + ex.getMessage());
            }
        return null;
    }

    /**
     * Converts this abstract pathname into a pathname string.  The resulting string uses the {@link #separator default name-separator character} to separate the names in the name sequence.
     *
     * @return The string form of this abstract pathname
     */
    @Override
    public String getPath() {
        return path.toString();
    }

    /**
     * Tests whether this abstract pathname is absolute.  The definition of absolute pathname is system dependent.  On UNIX systems, a pathname is absolute if its prefix is <code>"/"</code>.  On Microsoft Windows systems, a pathname is absolute if its prefix is a drive specifier followed by
     * <code>"\\"</code>, or if its prefix is <code>"\\\\"</code>.
     *
     * @return <code>true</code> if this abstract pathname is absolute,
     * <code>false</code> otherwise
     */
    @Override
    public boolean isAbsolute() {
        return path.isAbsolute();
    }

    /**
     * Returns the absolute pathname string of this abstract pathname.
     *
     * <p> If this abstract pathname is already absolute, then the pathname string is simply returned as if by the <code>{@link #getPath}</code> method.  If this abstract pathname is the empty abstract pathname then the pathname string of the current user directory, which is named by the system property <code>user.dir</code>, is returned.  Otherwise this pathname is resolved in a system-dependent way.  On UNIX systems, a relative pathname is made absolute by resolving it against the current user directory.  On Microsoft Windows systems, a relative pathname is made absolute by resolving it against the current directory of the drive named by the pathname, if any; if not, it is resolved against the current user directory.
     *
     * @return The absolute pathname string denoting the same file or directory as this abstract pathname
     * @throws SecurityException If a required system property value cannot be accessed.
     * @see java.io.File#isAbsolute()
     */
    @Override
    public String getAbsolutePath() {
        return path.resolve(path).toString();
    }

    /**
     * Returns the absolute path of this abstract pathname.
     *
     * <p> If this abstract pathname is already absolute, then the pathname string is simply returned as if by the <code>{@link #getPath}</code> method.  If this abstract pathname is the empty abstract pathname then the pathname string of the current user directory, which is named by the system property <code>user.dir</code>, is returned.  Otherwise this pathname is resolved in a system-dependent way.  On UNIX systems, a relative pathname is made absolute by resolving it against the current user directory.  On Microsoft Windows systems, a relative pathname is made absolute by resolving it against the current directory of the drive named by the pathname, if any; if not, it is resolved against the current user directory.
     *
     * @return The absolute path denoting the same file or directory as this abstract pathname
     * @throws SecurityException If a required system property value cannot be accessed.
     * @see java.io.File#isAbsolute()
     */
    private Path _getAbsolutePath() {
        return path.resolve(path);
    }

    /**
     * Returns the absolute form of this abstract pathname.  Equivalent to
     * <code>new&nbsp;File(this.{@link #getAbsolutePath})</code>.
     *
     * @return The absolute abstract pathname denoting the same file or directory as this abstract pathname
     * @throws SecurityException If a required system property value cannot be accessed.
     * @since 1.2
     */
    @Override
    public File getAbsoluteFile() {
        Path absPath = _getAbsolutePath();
        try {
            return new NioFile(absPath);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to get the absolute form of abstract pathname: " + absPath.toString() + ". Details: " + ex.getMessage());
            return new File(absPath.toString());
        }
    }

    /**
     * Returns the canonical pathname string of this abstract pathname.
     *
     * <p> A canonical pathname is both absolute and unique.  The precise definition of canonical form is system-dependent.  This method first converts this pathname to absolute form if necessary, as if by invoking the
     * {@link #getAbsolutePath} method, and then maps it to its unique form in a system-dependent way.  This typically involves removing redundant names such as <tt>"."</tt> and <tt>".."</tt> from the pathname, resolving symbolic links (on UNIX platforms), and converting drive letters to a standard case (on Microsoft Windows platforms).
     *
     * <p> Every pathname that denotes an existing file or directory has a unique canonical form.  Every pathname that denotes a nonexistent file or directory also has a unique canonical form.  The canonical form of the pathname of a nonexistent file or directory may be different from the canonical form of the same pathname after the file or directory is created.  Similarly, the canonical form of the pathname of an existing file or directory may be different from the canonical form of the same pathname after the file or directory is deleted.
     *
     * @return The canonical pathname string denoting the same file or directory as this abstract pathname
     * @throws IOException       If an I/O error occurs, which is possible because the construction of the canonical pathname may require filesystem queries
     * @throws SecurityException If a required system property value cannot be accessed, or if a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead}</code> method denies read access to the file
     * @see Path#toRealPath
     * @since JDK1.1
     */
    @Override
    public String getCanonicalPath() throws IOException {
        if (isInvalidPath()) {
            throw new IOException(INVALID_FILE_PATH_ERROR_MESSAGE);
        }
        return getAbsoluteFile().toPath().resolve(path).toString();
    }

    /**
     * Returns the canonical pathname string of this abstract pathname.
     *
     * <p> A canonical pathname is both absolute and unique.  The precise definition of canonical form is system-dependent.  This method first converts this pathname to absolute form if necessary, as if by invoking the
     * {@link #getAbsolutePath} method, and then maps it to its unique form in a system-dependent way.  This typically involves removing redundant names such as <tt>"."</tt> and <tt>".."</tt> from the pathname, resolving symbolic links (on UNIX platforms), and converting drive letters to a standard case (on Microsoft Windows platforms).
     *
     * <p> Every pathname that denotes an existing file or directory has a unique canonical form.  Every pathname that denotes a nonexistent file or directory also has a unique canonical form.  The canonical form of the pathname of a nonexistent file or directory may be different from the canonical form of the same pathname after the file or directory is created.  Similarly, the canonical form of the pathname of an existing file or directory may be different from the canonical form of the same pathname after the file or directory is deleted.
     *
     * @return The canonical path denoting the same file or directory as this abstract pathname
     * @throws IOException       If an I/O error occurs, which is possible because the construction of the canonical pathname may require filesystem queries
     * @throws SecurityException If a required system property value cannot be accessed, or if a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead}</code> method denies read access to the file
     * @see Path#toRealPath
     * @since JDK1.1
     */
    private Path getCanonicalPath0() throws IOException {
        if (isInvalidPath()) {
            throw new IOException(INVALID_FILE_PATH_ERROR_MESSAGE);
        }
        return getAbsoluteFile().toPath().resolve(path);
    }

    /**
     * Returns the canonical form of this abstract pathname.  Equivalent to
     * <code>new&nbsp;File(this.{@link #getCanonicalPath})</code>.
     *
     * @return The canonical pathname string denoting the same file or directory as this abstract pathname
     * @throws IOException       If an I/O error occurs, which is possible because the construction of the canonical pathname may require filesystem queries
     * @throws SecurityException If a required system property value cannot be accessed, or if a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead}</code> method denies read access to the file
     * @see Path#toRealPath
     * @since 1.2
     */
    @Override
    public File getCanonicalFile() throws IOException {
        Path canonPath = getCanonicalPath0();
        try {
            return new NioFile(canonPath);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to get the canonical form of abstract pathname:" + canonPath.toString() + ". Details: " + ex.getMessage());
            return new File(canonPath.toString());
        }
    }

    /**
     * Converts this abstract pathname into a <code>scheme:</code> URL.  The exact form of the URL is system-dependent.  If it can be determined that the file denoted by this abstract pathname is a directory, then the resulting URL will end with a slash.
     *
     * @return A URL object representing the equivalent file URL
     * @throws MalformedURLException If the path cannot be parsed as a URL
     * @see #toURI()
     * @see java.net.URI
     * @see java.net.URI#toURL()
     * @see java.net.URL
     * @since 1.2
     * @deprecated
     */
    @Deprecated
    @Override
    public URL toURL() throws MalformedURLException {
        if (isInvalidPath()) {
            throw new MalformedURLException(INVALID_FILE_PATH_ERROR_MESSAGE);
        }
        return new URL(path.toUri().getScheme(), "", slashify(_getAbsolutePath(), isDirectory()));
    }

    /**
     * Constructs a <tt>file:</tt> URI that represents this abstract pathname.
     *
     * <p> The exact form of the URI is system-dependent.  If it can be determined that the file denoted by this abstract pathname is a directory, then the resulting URI will end with a slash.
     *
     * <p> For a given abstract pathname <i>f</i>, it is guaranteed that
     *
     * <blockquote><tt> new File}(</tt><i>&nbsp;f</i><tt>.toURI()).equals(</tt><i>&nbsp;f</i><tt>.{@link #getAbsoluteFile() getAbsoluteFile}())
     * </tt></blockquote>
     * <p> so long as the original abstract pathname, the URI, and the new abstract pathname are all created in (possibly different invocations of) the same
     * Java virtual machine.  Due to the system-dependent nature of abstract pathnames, however, this relationship typically does not hold when a
     * <tt>file:</tt> URI that is created in a virtual machine on one operating system is converted into an abstract pathname in a virtual machine on a different operating system.
     *
     * <p> Note that when this abstract pathname represents a UNC pathname then all components of the UNC (including the server name component) are encoded in the {@code URI} path. The authority component is undefined, meaning that it is represented as {@code null}. The {@link Path} class defines the
     * {@link Path#toUri toUri} method to encode the server name in the authority component of the resulting {@code URI}. The {@link #toPath toPath} method may be used to obtain a {@code Path} representing this abstract pathname.
     *
     * @return An absolute, hierarchical URI with a scheme equal to
     * <tt>"file"</tt>, a path representing this abstract pathname, and undefined authority, query, and fragment components
     * @throws SecurityException If a required system property value cannot be accessed.
     * @see java.net.URI
     * @see java.net.URI#toURL()
     * @since 1.4
     */
    @Override
    public URI toURI() {
        return this.path.getFileSystem().getPath(slashify(_getAbsolutePath(), isDirectory())).toUri();
    }


    /* -- Attribute accessors -- */

    /**
     * Tests whether the application can read the file denoted by this abstract pathname. On some platforms it may be possible to start the
     * Java virtual machine with special privileges that allow it to read files that are marked as unreadable. Consequently this method may return
     * {@code true} even though the file does not have read permissions.
     *
     * @return <code>true</code> if and only if the file specified by this abstract pathname exists <em>and</em> can be read by the application; <code>false</code> otherwise
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file
     */
    @Override
    public boolean canRead() {
        return !isInvalidPath() && Files.isReadable(path);
    }

    /**
     * Tests whether the application can modify the file denoted by this abstract pathname. On some platforms it may be possible to start the
     * Java virtual machine with special privileges that allow it to modify files that are marked read-only. Consequently this method may return
     * {@code true} even though the file is marked read-only.
     *
     * @return <code>true</code> if and only if the file system actually contains a file denoted by this abstract pathname <em>and</em> the application is allowed to write to the file;
     * <code>false</code> otherwise.
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to the file
     */
    @Override
    public boolean canWrite() {
        return !isInvalidPath() && Files.isWritable(path);
    }

    /**
     * Tests whether the file or directory denoted by this abstract pathname exists.
     *
     * @return <code>true</code> if and only if the file or directory denoted by this abstract pathname exists; <code>false</code> otherwise
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file or directory
     */
    @Override
    public boolean exists() {
        return !isInvalidPath() && canRead() && Files.exists(path);
    }

    /**
     * Tests whether the file denoted by this abstract pathname is a directory.
     *
     * <p> Where it is required to distinguish an I/O exception from the case that the file is not a directory, or where several attributes of the same file are required at the same time, then the {@link java.nio.file.Files#readAttributes(Path, Class, LinkOption[])
     * Files.readAttributes} method may be used.
     *
     * @return <code>true</code> if and only if the file denoted by this abstract pathname exists <em>and</em> is a directory;
     * <code>false</code> otherwise
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file
     */
    @Override
    public boolean isDirectory() {
        return !isInvalidPath() && canRead() && Files.isDirectory(path);
    }

    /**
     * Tests whether the file denoted by this abstract pathname is a normal file.  A file is <em>normal</em> if it is not a directory and, in addition, satisfies other system-dependent criteria.  Any non-directory file created by a Java application is guaranteed to be a normal file.
     *
     * <p> Where it is required to distinguish an I/O exception from the case that the file is not a normal file, or where several attributes of the same file are required at the same time, then the {@link java.nio.file.Files#readAttributes(Path, Class, LinkOption[])
     * Files.readAttributes} method may be used.
     *
     * @return <code>true</code> if and only if the file denoted by this abstract pathname exists <em>and</em> is a normal file;
     * <code>false</code> otherwise
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file
     */
    @Override
    public boolean isFile() {
        return !isInvalidPath() && canRead() && Files.isRegularFile(path);
    }

    /**
     * Tests whether the file named by this abstract pathname is a hidden file.  The exact definition of <em>hidden</em> is system-dependent.  On
     * UNIX systems, a file is considered to be hidden if its name begins with a period character (<code>'.'</code>).  On Microsoft Windows systems, a file is considered to be hidden if it has been marked as such in the filesystem.
     *
     * @return <code>true</code> if and only if the file denoted by this abstract pathname is hidden according to the conventions of the underlying platform
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file
     * @since 1.2
     */
    @Override
    public boolean isHidden() {
        try {
            return !isInvalidPath() && canRead() && Files.isHidden(path);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to check if path: " + path.toString() + " is hidden. Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Returns the time that the file denoted by this abstract pathname was last modified.
     *
     * <p> Where it is required to distinguish an I/O exception from the case where {@code 0L} is returned, or where several attributes of the same file are required at the same time, or where the time of last access or the creation time are required, then the {@link java.nio.file.Files#readAttributes(Path, Class, LinkOption[])
     * Files.readAttributes} method may be used.
     *
     * @return A <code>long</code> value representing the time the file was last modified, measured in milliseconds since the epoch
     * (00:00:00 GMT, January 1, 1970), or <code>0L</code> if the file does not exist or if an I/O error occurs
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file
     */
    @Override
    public long lastModified() {
        if (isInvalidPath()) {
            return 0L;
        }
        try {
            if (!canRead()) {
                throw new SecurityException();
            }
            return Files.readAttributes(path, BasicFileAttributes.class).lastModifiedTime().toMillis();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to get the time that the file denoted by path: " + path.toString() + " was last modified. Details: " + ex.getMessage(), ex);
            return 0L;
        }
    }

    /**
     * Returns the length of the file denoted by this abstract pathname.
     * The return value is unspecified if this pathname denotes a directory.
     *
     * <p> Where it is required to distinguish an I/O exception from the case that {@code 0L} is returned, or where several attributes of the same file are required at the same time, then the {@link java.nio.file.Files#readAttributes(Path, Class, LinkOption[])
     * Files.readAttributes} method may be used.
     *
     * @return The length, in bytes, of the file denoted by this abstract pathname, or <code>0L</code> if the file does not exist.  Some operating systems may return <code>0L</code> for pathnames denoting system-dependent entities such as devices or pipes.
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file
     */
    @Override
    public long length() {
        if (isInvalidPath()) {
            return 0L;
        }
        try {
            if (!canRead()) {
                throw new SecurityException();
            }
            return Files.size(path);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to get the length of the file denoted by path: " + path.toString() + ". Details: " + ex.getMessage());
            return 0L;
        }
    }


    /* -- File operations -- */

    /**
     * Atomically creates a new, empty file named by this abstract pathname if and only if a file with this name does not yet exist.  The check for the existence of the file and the creation of the file if it does not exist are a single operation that is atomic with respect to all other filesystem activities that might affect the file.
     * <p>
     * Note: this method should <i>not</i> be used for file-locking, as the resulting protocol cannot be made to work reliably. The
     * {@link java.nio.channels.FileLock FileLock} facility should be used instead.
     *
     * @return <code>true</code> if the named file does not exist and was successfully created; <code>false</code> if the named file already exists
     * @throws IOException       If an I/O error occurred
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to the file
     * @since 1.2
     */
    @Override
    public boolean createNewFile() throws IOException {
        if (isInvalidPath()) {
            throw new IOException(INVALID_FILE_PATH_ERROR_MESSAGE);
        }
        Files.createFile(path);
        return canWrite();
    }

    /**
     * Deletes the file or directory denoted by this abstract pathname.  If this pathname denotes a directory, then the directory must be empty in order to be deleted.
     *
     * <p> Note that the {@link java.nio.file.Files} class defines the {@link java.nio.file.Files#delete(Path) delete} method to throw an {@link IOException} when a file cannot be deleted. This is useful for error reporting and to diagnose why a file cannot be deleted.
     *
     * @return <code>true</code> if and only if the file or directory is successfully deleted; <code>false</code> otherwise
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkDelete}</code> method denies delete access to the file
     */
    @Override
    public boolean delete() {
        if (isInvalidPath()) {
            return false;
        }
        try {
            return canWrite() && Files.deleteIfExists(path);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to delete the file or directory denoted by path: " + path.toString() + ". Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Requests that the file or directory denoted by this abstract pathname be deleted when the virtual machine terminates.
     * Files (or directories) are deleted in the reverse order that they are registered. Invoking this method to delete a file or directory that is already registered for deletion has no effect.
     * Deletion will be attempted only for normal termination of the virtual machine, as defined by the Java Language Specification.
     *
     * <p> Once deletion has been requested, it is not possible to cancel the request.  This method should therefore be used with care.
     *
     * <p>
     * Note: this method should <i>not</i> be used for file-locking, as the resulting protocol cannot be made to work reliably. The
     * {@link java.nio.channels.FileLock FileLock} facility should be used instead.
     *
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkDelete}</code> method denies delete access to the file
     * @see #delete
     * @since 1.2
     */
    @Override
    public void deleteOnExit() {
        throw new SecurityException(new UnsupportedOperationException());
    }

    /**
     * Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname that satisfy the specified filter.  The behavior of this method is the same as that of the {@link #list()} method, except that the strings in the returned array must satisfy the filter.  If the given {@code filter} is {@code null} then all names are accepted.  Otherwise, a name satisfies the filter if and only if the value {@code true} results when the {@link FilenameFilter#accept FilenameFilter.accept(File,&nbsp;String)} method of the filter is invoked on this abstract pathname and the name of a file or directory in the directory that it denotes.
     *
     * @param filter A DirectoryStream.Filter<Path> filter
     * @return An array of strings naming the files and directories in the directory denoted by this abstract pathname that were accepted by the given {@code filter}.  The array will be empty if the directory is empty or if no names were accepted by the filter.
     * Returns {@code null} if this abstract pathname does not denote a directory, or if an I/O error occurs.
     * @throws SecurityException If a security manager exists and its {@link SecurityManager#checkRead(String)} method denies read access to the directory
     * @see java.nio.file.Files#newDirectoryStream(Path, String)
     */
    public String[] list(DirectoryStream.Filter<Path> filter) {
        if (isInvalidPath()) {
            return new String[0];
        }
        List<String> files = new ArrayList<>();
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, filter)) {
                for (Path currentPath : stream) {
                    files.add(currentPath.getFileName().toString());
                }
                return files.toArray(new String[0]);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Unable to browse the path: " + path.toString() + " and get an array of strings naming the files and directories in the directory path. Details: " + ex.getMessage());
            }
        }
        return new String[0];
    }

    /**
     * Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname.
     *
     * <p> If this abstract pathname does not denote a directory, then this method returns {@code null}.  Otherwise an array of strings is returned, one for each file or directory in the directory.  Names denoting the directory itself and the directory's parent directory are not included in the result.  Each string is a file name rather than a complete path.
     *
     * <p> There is no guarantee that the name strings in the resulting array will appear in any specific order; they are not, in particular, guaranteed to appear in alphabetical order.
     *
     * <p> Note that the {@link java.nio.file.Files} class defines the {@link java.nio.file.Files#newDirectoryStream(Path) newDirectoryStream} method to open a directory and iterate over the names of the files in the directory.
     * This may use less resources when working with very large directories, and may be more responsive when working with remote directories.
     *
     * @return An array of strings naming the files and directories in the directory denoted by this abstract pathname.  The array will be empty if the directory is empty.  Returns {@code null} if this abstract pathname does not denote a directory, or if an
     * I/O error occurs.
     * @throws SecurityException If a security manager exists and its {@link
     *                           SecurityManager#checkRead(String)} method denies read access to the directory
     */
    @Override
    public String[] list() {
        DirectoryStream.Filter<Path> newFilter = entry -> true;
        return list(newFilter);
    }

    /**
     * Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname that satisfy the specified filter.  The behavior of this method is the same as that of the
     * {@link #list()} method, except that the strings in the returned array must satisfy the filter.  If the given {@code filter} is {@code null} then all names are accepted.  Otherwise, a name satisfies the filter if and only if the value {@code true} results when the {@link
     * FilenameFilter#accept FilenameFilter.accept(File,&nbsp;String)} method of the filter is invoked on this abstract pathname and the name of a file or directory in the directory that it denotes.
     *
     * @param filter A filename filter
     * @return An array of strings naming the files and directories in the directory denoted by this abstract pathname that were accepted by the given {@code filter}.  The array will be empty if the directory is empty or if no names were accepted by the filter.
     * Returns {@code null} if this abstract pathname does not denote a directory, or if an I/O error occurs.
     * @throws SecurityException If a security manager exists and its {@link
     *                           SecurityManager#checkRead(String)} method denies read access to the directory
     * @see java.nio.file.Files#newDirectoryStream(Path, String)
     */
    @Override
    public String[] list(FilenameFilter filter) {
        DirectoryStream.Filter<Path> newFilter = entry -> !(filter.accept(NioFile.this, new File(entry.toString()).getName()));
        return list(newFilter);
    }

    /**
     * Returns an array of abstract path names denoting the files and directories in the directory denoted by this abstract pathname that satisfy the specified filter.  The behavior of this method is the same as that of the {@link #listFiles()} method, except that the pathnames in the returned array must satisfy the filter.  If the given {@code filter} is {@code null} then all pathnames are accepted.  Otherwise, a pathname satisfies the filter if and only if the value {@code true} results when the {@link FileFilter#accept FileFilter.accept(File)} method of the filter is invoked on the pathname.
     *
     * @param filter A DirectoryStream.Filter<Path> filter
     * @return An array of abstract DirectoryStream.Filter<Path> denoting the files and directories in the directory denoted by this abstract pathname.
     * The array will be empty if the directory is empty.  Returns
     * {@code null} if this abstract pathname does not denote a directory, or if an I/O error occurs.
     * @throws SecurityException If a security manager exists and its {@link
     *                           SecurityManager#checkRead(String)} method denies read access to the directory
     * @see java.nio.file.Files#newDirectoryStream(Path, java.nio.file.DirectoryStream.Filter)
     * @since 1.2
     */
    private File[] listFiles(DirectoryStream.Filter<Path> filter) {
        List<File> files = new ArrayList<>();
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, filter)) {
                for (Path currentPath : stream) {
                    files.add(currentPath.toFile());
                }
                return files.toArray(new File[0]);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Unable to browse the path: " + path.toString() + " and get an array of path names denoting the files and directories in the directory path. Details: " + ex.getMessage());
            }
        }
        return new File[0];
    }

    /**
     * Returns an array of abstract pathnames denoting the files in the directory denoted by this abstract pathname.
     *
     * <p> If this abstract pathname does not denote a directory, then this method returns {@code null}.  Otherwise an array of {@code File} objects is returned, one for each file or directory in the directory.  Pathnames denoting the directory itself and the directory's parent directory are not included in the result. Therefore if this pathname is absolute then each resulting pathname is absolute; if this pathname is relative then each resulting pathname will be relative to the same directory.
     *
     * <p> There is no guarantee that the name strings in the resulting array will appear in any specific order; they are not, in particular, guaranteed to appear in alphabetical order.
     *
     * <p> Note that the {@link java.nio.file.Files} class defines the {@link java.nio.file.Files#newDirectoryStream(Path) newDirectoryStream} method to open a directory and iterate over the names of the files in the directory. This may use less resources when working with very large directories.
     *
     * @return An array of abstract pathnames denoting the files and directories in the directory denoted by this abstract pathname.
     * The array will be empty if the directory is empty.  Returns
     * {@code null} if this abstract pathname does not denote a directory, or if an I/O error occurs.
     * @throws SecurityException If a security manager exists and its {@link
     *                           SecurityManager#checkRead(String)} method denies read access to the directory
     * @since 1.2
     */
    @Override
    public File[] listFiles() {
        DirectoryStream.Filter<Path> newFilter = entry -> true;
        return listFiles(newFilter);
    }

    /**
     * Returns an array of abstract pathnames denoting the files and directories in the directory denoted by this abstract pathname that satisfy the specified filter.  The behavior of this method is the same as that of the {@link #listFiles()} method, except that the pathnames in the returned array must satisfy the filter.  If the given {@code filter} is {@code null} then all pathnames are accepted.  Otherwise, a pathname satisfies the filter if and only if the value {@code true} results when the {@link FilenameFilter#accept
     * FilenameFilter.accept(File,&nbsp;String)} method of the filter is invoked on this abstract pathname and the name of a file or directory in the directory that it denotes.
     *
     * @param filter A filename filter
     * @return An array of abstract pathnames denoting the files and directories in the directory denoted by this abstract pathname.
     * The array will be empty if the directory is empty.  Returns
     * {@code null} if this abstract pathname does not denote a directory, or if an I/O error occurs.
     * @throws SecurityException If a security manager exists and its {@link
     *                           SecurityManager#checkRead(String)} method denies read access to the directory
     * @see java.nio.file.Files#newDirectoryStream(Path, String)
     * @since 1.2
     */
    @Override
    public File[] listFiles(FilenameFilter filter) {
        DirectoryStream.Filter<Path> newFilter = entry -> !(filter.accept(NioFile.this, new File(entry.toString()).getName()));
        return listFiles(newFilter);
    }

    /**
     * Returns an array of abstract pathnames denoting the files and directories in the directory denoted by this abstract pathname that satisfy the specified filter.  The behavior of this method is the same as that of the {@link #listFiles()} method, except that the pathnames in the returned array must satisfy the filter.  If the given {@code filter} is {@code null} then all pathnames are accepted.  Otherwise, a pathname satisfies the filter if and only if the value {@code true} results when the {@link FileFilter#accept FileFilter.accept(File)} method of the filter is invoked on the pathname.
     *
     * @param filter A file filter
     * @return An array of abstract pathnames denoting the files and directories in the directory denoted by this abstract pathname.
     * The array will be empty if the directory is empty.  Returns
     * {@code null} if this abstract pathname does not denote a directory, or if an I/O error occurs.
     * @throws SecurityException If a security manager exists and its {@link
     *                           SecurityManager#checkRead(String)} method denies read access to the directory
     * @see java.nio.file.Files#newDirectoryStream(Path, java.nio.file.DirectoryStream.Filter)
     * @since 1.2
     */
    @Override
    public File[] listFiles(FileFilter filter) {
        DirectoryStream.Filter<Path> newFilter = entry -> !(filter.accept(new File(entry.toString())));
        return listFiles(newFilter);
    }

    /**
     * Creates the directory named by this abstract pathname.
     *
     * @return <code>true</code> if and only if the directory was created; <code>false</code> otherwise
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method does not permit the named directory to be created
     */
    @Override
    public boolean mkdir() {
        try {
            Files.createDirectory(path);
            return canRead();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to create the directory named by abstract pathname:" + path.toString() + ". Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories.  Note that if this operation fails it may have succeeded in creating some of the necessary parent directories.
     *
     * @return <code>true</code> if and only if the directory was created, along with all necessary parent directories; <code>false</code> otherwise
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method does not permit verification of the existence of the named directory and all necessary parent directories; or if the <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method does not permit the named directory and all necessary parent directories to be created
     */
    @Override
    public boolean mkdirs() {
        if (exists()) {
            return false;
        }
        if (mkdir()) {
            return true;
        }
        File canonFile;
        try {
            canonFile = getCanonicalFile();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to create the directory named by abstract pathname:" + path.toString() + ", including any necessary but nonexistent parent directories. Details: " + ex.getMessage());
            return false;
        }

        File parent = canonFile.getParentFile();
        return (parent != null && (parent.mkdirs() || parent.exists()) &&
                canonFile.mkdir());
    }

    /**
     * Renames the file denoted by this abstract pathname.
     *
     * <p> Many aspects of the behavior of this method are inherently platform-dependent: The rename operation might not be able to move a file from one filesystem to another, it might not be atomic, and it might not succeed if a file with the destination abstract pathname already exists.  The return value should always be checked to make sure that the rename operation was successful.
     *
     * <p> Note that the {@link java.nio.file.Files} class defines the {@link java.nio.file.Files#move move} method to move or rename a file in a platform independent manner.
     *
     * @param dest The new abstract pathname for the named file
     * @return <code>true</code> if and only if the renaming succeeded;
     * <code>false</code> otherwise
     * @throws SecurityException    If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to either the old or new pathnames
     * @throws NullPointerException If parameter <code>dest</code> is <code>null</code>
     */
    @Override
    public boolean renameTo(File dest) {
        if (dest == null) {
            throw new NullPointerException();
        }
        try {
            return !this.isInvalidPath() && !((NioFile) dest).isInvalidPath() && !Files.move(path, path.resolveSibling(dest.toPath())).toString().isEmpty();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to rename the directory named by abstract pathname:" + path.toString() + ". Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Sets the last-modified time of the file or directory named by this abstract pathname.
     *
     * <p> All platforms support file-modification times to the nearest second, but some provide more precision.  The argument will be truncated to fit the supported precision.  If the operation succeeds and no intervening operations on the file take place, then the next invocation of the
     * <code>{@link #lastModified}</code> method will return the (possibly truncated) <code>time</code> argument that was passed to this method.
     *
     * @param time The new last-modified time, measured in milliseconds since the epoch (00:00:00 GMT, January 1, 1970)
     * @return <code>true</code> if and only if the operation succeeded;
     * <code>false</code> otherwise
     * @throws IllegalArgumentException If the argument is negative
     * @throws SecurityException        If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to the named file
     * @since 1.2
     */
    @Override
    public boolean setLastModified(long time) {
        if (time < 0) throw new IllegalArgumentException("Negative time");
        try {
            Files.setLastModifiedTime(path, FileTime.fromMillis(time));
            return !isInvalidPath();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to set last-modified time of the file or directory named by abstract pathname:" + path.toString() + ". Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Marks the file or directory named by this abstract pathname so that only read operations are allowed. After invoking this method the file or directory will not change until it is either deleted or marked to allow write access. On some platforms it may be possible to start the
     * Java virtual machine with special privileges that allow it to modify files that are marked read-only. Whether or not a read-only file or directory may be deleted depends upon the underlying system.
     *
     * @return <code>true</code> if and only if the operation succeeded;
     * <code>false</code> otherwise
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to the named file
     * @since 1.2
     */
    @Override
    public boolean setReadOnly() {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);
            return !isInvalidPath();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to mark the file or directory named by abstract pathname:" + path.toString() + ", so that only read operations are allowed. Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Sets the owner's or everybody's write permission for this abstract pathname. On some platforms it may be possible to start the Java virtual machine with special privileges that allow it to modify files that disallow write operations.
     *
     * <p> The {@link java.nio.file.Files} class defines methods that operate on file attributes including file permissions. This may be used when finer manipulation of file permissions is required.
     *
     * @param writable  If <code>true</code>, sets the access permission to allow write operations; if <code>false</code> to disallow write operations
     * @param ownerOnly If <code>true</code>, the write permission applies only to the owner's write permission; otherwise, it applies to everybody.  If the underlying file system can not distinguish the owner's write permission from that of others, then the permission will apply to everybody, regardless of this value.
     * @return <code>true</code> if and only if the operation succeeded. The operation will fail if the user does not have permission to change the access permissions of this abstract pathname.
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to the named file
     * @since 1.6
     */
    @Override
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            if (writable) {
                perms.add(PosixFilePermission.OWNER_WRITE);
                if (!ownerOnly) {
                    perms.add(PosixFilePermission.GROUP_WRITE);
                    perms.add(PosixFilePermission.OTHERS_WRITE);
                }
            }
            Files.setPosixFilePermissions(path, perms);
            return !isInvalidPath();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable set the owner's or everybody's write permission for the file or directory named by abstract pathname:" + path.toString() + ". Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * A convenience method to set the owner's write permission for this abstract pathname. On some platforms it may be possible to start the Java virtual machine with special privileges that allow it to modify files that disallow write operations.
     *
     * <p> An invocation of this method of the form <tt>file.setWritable(arg)</tt> behaves in exactly the same way as the invocation
     *
     * <pre> file.setWritable(arg, true) </pre>
     *
     * @param writable If <code>true</code>, sets the access permission to allow write operations; if <code>false</code> to disallow write operations
     * @return <code>true</code> if and only if the operation succeeded.  The operation will fail if the user does not have permission to change the access permissions of this abstract pathname.
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to the file
     * @since 1.6
     */
    @Override
    public boolean setWritable(boolean writable) {
        return setWritable(writable, true);
    }

    /**
     * Sets the owner's or everybody's read permission for this abstract pathname. On some platforms it may be possible to start the Java virtual machine with special privileges that allow it to read files that are marked as unreadable.
     *
     * <p> The {@link java.nio.file.Files} class defines methods that operate on file attributes including file permissions. This may be used when finer manipulation of file permissions is required.
     *
     * @param readable  If <code>true</code>, sets the access permission to allow read operations; if <code>false</code> to disallow read operations
     * @param ownerOnly If <code>true</code>, the read permission applies only to the owner's read permission; otherwise, it applies to everybody.  If the underlying file system can not distinguish the owner's read permission from that of others, then the permission will apply to everybody, regardless of this value.
     * @return <code>true</code> if and only if the operation succeeded.  The operation will fail if the user does not have permission to change the access permissions of this abstract pathname.  If
     * <code>readable</code> is <code>false</code> and the underlying file system does not implement a read permission, then the operation will fail.
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to the file
     * @since 1.6
     */
    @Override
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            if (readable) {
                perms.add(PosixFilePermission.OWNER_READ);
                if (!ownerOnly) {
                    perms.add(PosixFilePermission.GROUP_READ);
                    perms.add(PosixFilePermission.OTHERS_READ);
                }
            }
            Files.setPosixFilePermissions(path, perms);
            return !isInvalidPath();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable set the owner's or everybody's read permission for the file or directory named by abstract pathname:" + path.toString() + ". Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * A convenience method to set the owner's read permission for this abstract pathname. On some platforms it may be possible to start the Java virtual machine with special privileges that allow it to read files that that are marked as unreadable.
     *
     * <p>An invocation of this method of the form <tt>file.setReadable(arg)</tt> behaves in exactly the same way as the invocation
     *
     * <pre> file.setReadable(arg, true) </pre>
     *
     * @param readable If <code>true</code>, sets the access permission to allow read operations; if <code>false</code> to disallow read operations
     * @return <code>true</code> if and only if the operation succeeded.  The operation will fail if the user does not have permission to change the access permissions of this abstract pathname.  If
     * <code>readable</code> is <code>false</code> and the underlying file system does not implement a read permission, then the operation will fail.
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to the file
     * @since 1.6
     */
    @Override
    public boolean setReadable(boolean readable) {
        return setReadable(readable, true);
    }

    /**
     * Sets the owner's or everybody's execute permission for this abstract pathname. On some platforms it may be possible to start the Java virtual machine with special privileges that allow it to execute files that are not marked executable.
     *
     * <p> The {@link java.nio.file.Files} class defines methods that operate on file attributes including file permissions. This may be used when finer manipulation of file permissions is required.
     *
     * @param executable If <code>true</code>, sets the access permission to allow execute operations; if <code>false</code> to disallow execute operations
     * @param ownerOnly  If <code>true</code>, the execute permission applies only to the owner's execute permission; otherwise, it applies to everybody.
     *                   If the underlying file system can not distinguish the owner's execute permission from that of others, then the permission will apply to everybody, regardless of this value.
     * @return <code>true</code> if and only if the operation succeeded.  The operation will fail if the user does not have permission to change the access permissions of this abstract pathname.  If
     * <code>executable</code> is <code>false</code> and the underlying file system does not implement an execute permission, then the operation will fail.
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to the file
     * @since 1.6
     */
    @Override
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            if (executable) {
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                if (!ownerOnly) {
                    perms.add(PosixFilePermission.GROUP_EXECUTE);
                    perms.add(PosixFilePermission.OTHERS_EXECUTE);
                }
            }
            Files.setPosixFilePermissions(path, perms);
            return !isInvalidPath();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable set the owner's or everybody's execute permission for the file or directory named by abstract pathname:" + path.toString() + ". Details: " + ex.getMessage());
            return false;
        }
    }

    /**
     * A convenience method to set the owner's execute permission for this abstract pathname. On some platforms it may be possible to start the Java virtual machine with special privileges that allow it to execute files that are not marked executable.
     *
     * <p>An invocation of this method of the form <tt>file.setExcutable(arg)</tt> behaves in exactly the same way as the invocation
     *
     * <pre> file.setExecutable(arg, true) </pre>
     *
     * @param executable If <code>true</code>, sets the access permission to allow execute operations; if <code>false</code> to disallow execute operations
     * @return <code>true</code> if and only if the operation succeeded.  The operation will fail if the user does not have permission to change the access permissions of this abstract pathname.  If
     * <code>executable</code> is <code>false</code> and the underlying file system does not implement an execute permission, then the operation will fail.
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkWrite(java.lang.String)}</code> method denies write access to the file
     * @since 1.6
     */
    @Override
    public boolean setExecutable(boolean executable) {
        return setExecutable(executable, true);
    }

    /**
     * Tests whether the application can execute the file denoted by this abstract pathname. On some platforms it may be possible to start the
     * Java virtual machine with special privileges that allow it to execute files that are not marked executable. Consequently this method may return
     * {@code true} even though the file does not have execute permissions.
     *
     * @return <code>true</code> if and only if the abstract pathname exists
     * <em>and</em> the application is allowed to execute the file
     * @throws SecurityException If a security manager exists and its <code>{@link java.lang.SecurityManager#checkExec(java.lang.String)}</code> method denies execute access to the file
     * @since 1.6
     */
    @Override
    public boolean canExecute() {
        return !isInvalidPath() && Files.isExecutable(path);
    }


    /* -- Filesystem interface -- */

    /**
     * Returns the size of the partition <a href="#partName">named</a> by this abstract pathname.
     *
     * @return The size, in bytes, of the partition or <tt>0L</tt> if this abstract pathname does not name a partition
     * @throws SecurityException If a security manager has been installed and it denies
     *                           {@link RuntimePermission}<tt>("getFileSystemAttributes")</tt> or its {@link SecurityManager#checkRead(String)} method denies read access to the file named by this abstract pathname
     * @since 1.6
     */
    @Override
    public long getTotalSpace() {
        if (isInvalidPath() || !canRead()) {
            return 0L;
        }
        try {
            return this.path.getFileSystem().provider().getFileStore(path).getTotalSpace();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to get the size of physical memory unit where file or directory named by abstract pathname:" + path.toString() + " exists. Details: " + ex.getMessage());
            return 0L;
        }
    }


    /* -- Disk usage -- */

    /**
     * Returns the number of unallocated bytes in the partition <a href="#partName">named</a> by this abstract path name.
     *
     * <p> The returned number of unallocated bytes is a hint, but not a guarantee, that it is possible to use most or any of these bytes.  The number of unallocated bytes is most likely to be accurate immediately after this call.  It is likely to be made inaccurate by any external I/O operations including those made on the system outside of this virtual machine.  This method makes no guarantee that write operations to this file system will succeed.
     *
     * @return The number of unallocated bytes on the partition or <tt>0L</tt> if the abstract pathname does not name a partition.  This value will be less than or equal to the total file system size returned by {@link #getTotalSpace}.
     * @throws SecurityException If a security manager has been installed and it denies
     *                           {@link RuntimePermission}<tt>("getFileSystemAttributes")</tt> or its {@link SecurityManager#checkRead(String)} method denies read access to the file named by this abstract pathname
     * @since 1.6
     */
    @Override
    public long getFreeSpace() {
        if (isInvalidPath() || !canRead()) {
            return 0L;
        }
        try {
            return this.path.getFileSystem().provider().getFileStore(path).getUnallocatedSpace();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to get the number of unallocated bytes in the physical memory unit where file or directory named by abstract pathname:" + path.toString() + " exists. Details: " + ex.getMessage());
            return 0L;
        }
    }

    /**
     * Returns the number of bytes available to this virtual machine on the partition <a href="#partName">named</a> by this abstract pathname.  When possible, this method checks for write permissions and other operating system restrictions and will therefore usually provide a more accurate estimate of how much new data can actually be written than {@link
     * #getFreeSpace}.
     *
     * <p> The returned number of available bytes is a hint, but not a guarantee, that it is possible to use most or any of these bytes.  The number of unallocated bytes is most likely to be accurate immediately after this call.  It is likely to be made inaccurate by any external
     * I/O operations including those made on the system outside of this virtual machine.  This method makes no guarantee that write operations to this file system will succeed.
     *
     * @return The number of available bytes on the partition or <tt>0L</tt> if the abstract pathname does not name a partition.  On systems where this information is not available, this method will be equivalent to a call to {@link #getFreeSpace}.
     * @throws SecurityException If a security manager has been installed and it denies
     *                           {@link RuntimePermission}<tt>("getFileSystemAttributes")</tt> or its {@link SecurityManager#checkRead(String)} method denies read access to the file named by this abstract pathname
     * @since 1.6
     */
    @Override
    public long getUsableSpace() {
        if (isInvalidPath() || !canRead()) {
            return 0L;
        }
        try {
            return this.path.getFileSystem().provider().getFileStore(path).getUsableSpace();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to get the number of bytes available to this virtual machine in the physical memory unit where file or directory named by abstract pathname:" + path.toString() + " exists. Details: " + ex.getMessage());
            return 0L;
        }
    }

    /**
     * Compares two abstract pathnames lexicographically.  The ordering defined by this method depends upon the underlying system.  On UNIX systems, alphabetic case is significant in comparing pathnames; on Microsoft Windows systems it is not.
     *
     * @param pathname The abstract pathname to be compared to this abstract pathname
     * @return Zero if the argument is equal to this abstract pathname, a value less than zero if this abstract pathname is lexicographically less than the argument, or a value greater than zero if this abstract pathname is lexicographically greater than the argument
     * @since 1.2
     */
    @Override
    public int compareTo(File pathname) {
        return path.compareTo(pathname.toPath());
    }


    /* -- Basic infrastructure -- */

    /**
     * Tests this abstract pathname for equality with the given object.
     * Returns <code>true</code> if and only if the argument is not
     * <code>null</code> and is an abstract pathname that denotes the same file or directory as this abstract pathname.  Whether or not two abstract pathnames are equal depends upon the underlying system.  On UNIX systems, alphabetic case is significant in comparing pathnames; on Microsoft Windows systems it is not.
     *
     * @param obj The object to be compared with this abstract pathname
     * @return <code>true</code> if and only if the objects are the same;
     * <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof NioFile)) {
            return path.getClass().equals(((NioFile) obj).path.getClass()) && compareTo((File) obj) == 0;
        }
        return false;
    }

    /**
     * Computes a hash code for this abstract pathname.  Because equality of abstract pathnames is inherently system-dependent, so is the computation of their hash codes.  On UNIX systems, the hash code of an abstract pathname is equal to the exclusive <em>or</em> of the hash code of its pathname string and the decimal value
     * <code>1234321</code>.  On Microsoft Windows systems, the hash code is equal to the exclusive <em>or</em> of the hash code of its pathname string converted to lower case and the decimal value <code>1234321</code>.  Locale is not taken into account on lowercasing the pathname string.
     *
     * @return A hash code for this abstract pathname
     */
    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /**
     * Returns the pathname string of this abstract pathname.  This is just the string returned by the <code>{@link #getPath}</code> method.
     *
     * @return The string form of this abstract pathname
     */
    @Override
    public String toString() {
        return getPath();
    }

    /**
     * Returns a {@link Path java.nio.file.Path} object constructed from the this abstract path.
     *
     * @return a {@code Path} constructed from this abstract path
     * @throws java.nio.file.InvalidPathException if a {@code Path} object cannot be constructed from the abstract path (see {@link java.nio.file.FileSystem#getPath FileSystem.getPath})
     * @see Path#toFile
     * @since 1.7
     */
    @Override
    public Path toPath() {
        return this.path;
    }

    /**
     * Enum type that indicates the status of a file path.
     */
    private enum PathStatus {
        INVALID, CHECKED
    }
}