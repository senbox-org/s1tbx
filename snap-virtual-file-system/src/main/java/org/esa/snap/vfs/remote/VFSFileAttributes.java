package org.esa.snap.vfs.remote;

import org.esa.snap.vfs.remote.http.RegularFileMetadataCallback;

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
 * File Attributes for VFS.
 * Basic attributes associated with a file or directory in a file system.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public abstract class VFSFileAttributes implements BasicFileAttributes {

    /**
     * The default file time for a file is EPOCH.
     */
    static final FileTime UNKNOWN_FILE_TIME = FileTime.from(Instant.EPOCH);
    /**
     * The date-time format used.
     */
    static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'");
    private static final Logger logger = Logger.getLogger(VFSFileAttributes.class.getName());

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
        return newFile(fileKey, new RegularFileMetadata(lastModified, size));
    }

    public static BasicFileAttributes newFile(String fileKey, RegularFileMetadata regularFileMetadata) {
        if (fileKey == null) {
            throw new NullPointerException("fileKey is null");
        }
        if (regularFileMetadata == null) {
            throw new NullPointerException("regularFileMetadata is null");
        }
        return new RegularFileAttributes(fileKey, regularFileMetadata, null);
    }

    public static BasicFileAttributes newFile(String fileKey, RegularFileMetadataCallback regularFileMetadataCallback) {
        if (fileKey == null) {
            throw new NullPointerException("fileKey is null");
        }
        if (regularFileMetadataCallback == null) {
            throw new NullPointerException("regularFileMetadataCallback is null");
        }
        return new RegularFileAttributes(fileKey, null, regularFileMetadataCallback);
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
     * Returns the dir attributes for VFS root directory
     *
     * @return The new dir attributes
     */
    static BasicFileAttributes getRoot() {
        return new DirAttributes("/");
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

    public String fileURL() {
        return null;
    }

    /**
     * Regular File Attributes for VFS.
     * Basic attributes associated with a file in a file system.
     *
     * @author Norman Fomferra
     * @author Adrian Drăghici
     */
    public static class RegularFileAttributes extends VFSFileAttributes {

        private final String fileKey;
        private final RegularFileMetadataCallback fileSizeQueryCallback;

        private FileTime lastModifiedTime;
        private RegularFileMetadata regularFileMetadata;

        /**
         * Creates new basic file attributes for a VFS file.
         *
         * @param fileKey The unique identifier of file
         */
        RegularFileAttributes(String fileKey, RegularFileMetadata regularFileMetadata, RegularFileMetadataCallback fileSizeQueryCallback) {
            this.fileKey = fileKey;
            this.regularFileMetadata = regularFileMetadata;
            this.fileSizeQueryCallback = fileSizeQueryCallback;
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
         * Returns an object that uniquely identifies the given file, or {@code null} if a file key is not available.
         *
         * @return The object that uniquely identifies the given file, or {@code null}
         */
        @Override
        public String fileKey() {
            return this.fileKey;
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
        public synchronized long size() {
            readMetadataIfNeeded();
            return this.regularFileMetadata.getSize();
        }

        /**
         * Returns the time of last modification.
         *
         * @return The time the file was last modified
         */
        @Override
        public synchronized FileTime lastModifiedTime() {
            readMetadataIfNeeded();
            if (this.lastModifiedTime == null) {
                this.lastModifiedTime = UNKNOWN_FILE_TIME;
                String lastModified = this.regularFileMetadata.getLastModified();
                if (lastModified != null) {
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(lastModified, ISO_DATE_TIME);
                        this.lastModifiedTime = FileTime.from(dateTime.toInstant(ZoneOffset.UTC));
                    } catch (DateTimeParseException ex) {
                        logger.log(Level.FINE, "Unable to convert the string: " + lastModified + " to LocalDateTime. Details: " + ex.getMessage());
                    }
                }
            }
            return this.lastModifiedTime;
        }

        private void readMetadataIfNeeded() {
            if (this.regularFileMetadata == null) {
                try {
                    this.regularFileMetadata = this.fileSizeQueryCallback.readFileMetadata();
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to read the metadata for file '" + this.fileKey + "'.", e);
                }
            }
        }

        @Override
        public String fileURL() {
            readMetadataIfNeeded();
            if (this.regularFileMetadata != null) {
                return this.regularFileMetadata.getFileURL();
            }
            return super.fileURL();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RegularFileAttributes) {
                return this.fileKey.contentEquals(((RegularFileAttributes) obj).fileKey);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.fileKey.hashCode();
        }
    }

    /**
     * Dir Attributes for VFS.
     * Basic attributes associated with a directory in a file system.
     *
     * @author Norman Fomferra
     * @author Adrian Drăghici
     */
    private static class DirAttributes extends VFSFileAttributes {

        private final String fileKey;

        /**
         * Creates new basic file attributes for a VFS directory.
         *
         * @param fileKey The VFS path of directory
         */
        DirAttributes(String fileKey) {
            this.fileKey = fileKey;
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
         * Returns an object that uniquely identifies the given file, or {@code null} if a file key is not available.
         *
         * @return The object that uniquely identifies the given file, or {@code null}
         */
        @Override
        public String fileKey() {
            return this.fileKey;
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

        /**
         * Returns the size of the file (in bytes). The size may differ from the actual size on the file system due to compression, support for sparse files, or other reasons. The size of files that are not {@link #isRegularFile regular} files is implementation specific and therefore unspecified.
         *
         * @return The file size, in bytes
         */
        @Override
        public long size() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DirAttributes) {
                return this.fileKey.contentEquals(((DirAttributes) obj).fileKey);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.fileKey.hashCode();
        }
    }
}
