package org.esa.snap.vfs.remote;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File Attributes for Object Storage VFS.
 * Basic attributes associated with a file or directory in a file system.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public abstract class ObjectStorageFileAttributes implements BasicFileAttributes {

    /**
     * The default file time for a file is EPOCH.
     */
    static final FileTime UNKNOWN_FILE_TIME = FileTime.from(Instant.EPOCH);

    /**
     * The date-time format used.
     */
    static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'");

    private static Logger logger = Logger.getLogger(ObjectStorageFileAttributes.class.getName());

    /**
     * Extract basic file attributes from the given VFS path
     *
     * @param path the VFS path
     * @return The basic file attributes
     * @throws IOException If an I/O error occurs
     */
    static BasicFileAttributes fromPath(ObjectStoragePath path) throws IOException {
        BasicFileAttributes fileAttributes = path.getFileAttributes();
        if (fileAttributes == null) {
            if (path.isDirectory()) {
                fileAttributes = newDir(path.toString().substring(1));
            } else {
                fileAttributes = ((AbstractRemoteFileSystemProvider) path.getFileSystem().provider()).newObjectStorageWalker().getObjectStorageFile(path.getFileURL().toString(), path.toString());
            }
            path.setFileAttributes(fileAttributes);
        }
        return fileAttributes;
    }

    /**
     * Creates new basic file attributes for a VFS file.
     *
     * @param fileKey      The unique identifier of file
     * @param size         The size of file
     * @param lastModified The time of last file change
     * @return new basic file attributes
     * @see RegularFileAttributes
     */
    public static BasicFileAttributes newFile(String fileKey, long size, String lastModified) {
        return new RegularFileAttributes(fileKey, lastModified, size);
    }

    /**
     * Creates new basic file attributes for a VFS directory.
     *
     * @param prefix The VFS path of directory
     * @return new basic file attributes
     * @see DirAttributes
     */
    public static BasicFileAttributes newDir(String prefix) {
        return new DirAttributes(prefix);
    }

    /**
     * Returns the size of the file (in bytes). The size may differ from the actual size on the file system due to compression, support for sparse files, or other reasons. The size of files that are not {@link #isRegularFile regular} files is implementation specific and therefore unspecified.
     *
     * @return The file size, in bytes
     */
    @Override
    public long size() {
        return 0;
    }

    /**
     * Returns the time of last modification.
     *
     * @return The time the file was last modified
     */
    @Override
    public FileTime lastModifiedTime() {
        return UNKNOWN_FILE_TIME;
    }

    /**
     * Returns the time of last access.
     *
     * @return The time of last access
     */
    @Override
    public FileTime lastAccessTime() {
        return UNKNOWN_FILE_TIME;
    }

    /**
     * Returns the creation time. The creation time is the time that the file was created.
     *
     * @return The time the file was created
     */
    @Override
    public FileTime creationTime() {
        return UNKNOWN_FILE_TIME;
    }

    /**
     * Tells whether the file is a regular file with opaque content.
     *
     * @return {@code true} if the file is a regular file with opaque content
     */
    @Override
    public boolean isRegularFile() {
        return false;
    }

    /**
     * Tells whether the file is a directory.
     *
     * @return {@code true} if the file is a directory
     */
    @Override
    public boolean isDirectory() {
        return false;
    }

    /**
     * Tells whether the file is a symbolic link.
     *
     * @return {@code true} if the file is a symbolic link
     */
    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    /**
     * Tells whether the file is something other than a regular file, directory, or symbolic link.
     *
     * @return {@code true} if the file something other than a regular file, directory or symbolic link
     */
    @Override
    public boolean isOther() {
        return false;
    }

    /**
     * Regular File Attributes for Object Storage VFS.
     * Basic attributes associated with a file in a file system.
     *
     * @author Norman Fomferra
     * @author Adrian Drăghici
     */
    public static class RegularFileAttributes extends ObjectStorageFileAttributes {

        private final String fileKey;
        private final long size;
        private final String lastModified;
        private FileTime lastModifiedTime;

        /**
         * Creates new basic file attributes for a VFS file.
         *
         * @param fileKey      The unique identifier of file
         * @param size         The size of file
         * @param lastModified The time of last file change
         */
        RegularFileAttributes(String fileKey, String lastModified, long size) {
            this.fileKey = fileKey;
            this.size = size;
            this.lastModified = lastModified;
        }

        /**
         * Returns an object that uniquely identifies the given file, or {@code null} if a file key is not available.
         *
         * @return The object that uniquely identifies the given file, or {@code null}
         */
        @Override
        public Object fileKey() {
            return fileKey;
        }

        /**
         * Tells whether the file is a regular file with opaque content.
         *
         * @return {@code true} if the file is a regular file with opaque content
         */
        @Override
        public boolean isRegularFile() {
            return true;
        }

        /**
         * Returns the size of the file (in bytes). The size may differ from the actual size on the file system due to compression, support for sparse files, or other reasons. The size of files that are not {@link #isRegularFile regular} files is implementation specific and therefore unspecified.
         *
         * @return The file size, in bytes
         */
        @Override
        public long size() {
            return size;
        }

        /**
         * Returns the time of last modification.
         *
         * @return The time the file was last modified
         */
        @Override
        public synchronized FileTime lastModifiedTime() {
            if (lastModifiedTime == null) {
                lastModifiedTime = UNKNOWN_FILE_TIME;
                if (lastModified != null) {
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(lastModified, ISO_DATE_TIME);
                        lastModifiedTime = FileTime.from(dateTime.toInstant(ZoneOffset.UTC));
                    } catch (DateTimeParseException ex) {
                        logger.log(Level.FINE, "Unable to convert the string: " + lastModified + " to LocalDateTime. Details: " + ex.getMessage());
                    }
                }
            }
            return lastModifiedTime;
        }
    }

    /**
     * Dir Attributes for Object Storage VFS.
     * Basic attributes associated with a directory in a file system.
     *
     * @author Norman Fomferra
     * @author Adrian Drăghici
     */
    private static class DirAttributes extends ObjectStorageFileAttributes {

        private final String prefix;

        /**
         * Creates new basic file attributes for a VFS directory.
         *
         * @param prefix The VFS path of directory
         */
        DirAttributes(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Returns an object that uniquely identifies the given file, or {@code null} if a file key is not available.
         *
         * @return The object that uniquely identifies the given file, or {@code null}
         */
        @Override
        public Object fileKey() {
            return prefix;
        }

        /**
         * Tells whether the file is a directory.
         *
         * @return {@code true} if the file is a directory
         */
        @Override
        public boolean isDirectory() {
            return true;
        }
    }

    /**
     * Empty Attributes for Object Storage VFS.
     *
     * @author Norman Fomferra
     * @author Adrian Drăghici
     */
    private static class EmptyAttributes extends ObjectStorageFileAttributes {

        /**
         * Returns an object that uniquely identifies the given file, or {@code null} if a file key is not available.
         *
         * @return The object that uniquely identifies the given file, or {@code null}
         */
        @Override
        public Object fileKey() {
            return "";
        }
    }

    /**
     * Returns the dir attributes for VFS root directory
     *
     * @return The new dir attributes
     */
    static BasicFileAttributes getRoot() {
        return new DirAttributes("/");
    }

    /**
     * Returns the empty basic file attributes
     *
     * @return The new basic file attributes
     */
    static BasicFileAttributes getEmpty() {
        return new EmptyAttributes();
    }

}
