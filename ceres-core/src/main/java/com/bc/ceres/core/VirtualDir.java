/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// todo - fully support "." and ".." directories.

/**
 * A read-only directory that can either be a directory in the file system or a ZIP-file.
 * Files having '.gz' extensions are automatically decompressed.
 *
 * @author Norman Fomferra
 * @since Ceres 0.11
 */
public abstract class VirtualDir {

    /**
     * @return The base path name of the directory.
     */
    public abstract String getBasePath();

    /**
     * Opens a reader for the given relative path.
     *
     * @param path The relative file path.
     *
     * @return A reader for the specified relative path.
     *
     * @throws IOException If the file does not exist or if it can't be opened for reading.
     */
    public Reader getReader(String path) throws IOException {
        return new InputStreamReader(getInputStream(path));
    }

    /**
     * Opens an input stream for the given relative file path.
     * Files having '.gz' extensions are automatically decompressed.
     *
     * @param path The relative file path.
     *
     * @return An input stream for the specified relative path.
     *
     * @throws IOException If the file does not exist or if it can't be opened for reading.
     */
    public abstract InputStream getInputStream(String path) throws IOException;

    /**
     * Gets the file for the given relative path.
     *
     * @param path The relative file or directory path.
     *
     * @return Gets the file or directory for the specified file path.
     *
     * @throws IOException If the file or directory does not exist or if it can't be extracted from a ZIP-file.
     */
    public abstract File getFile(String path) throws IOException;

    /**
     * Returns an array of strings naming the files and directories in the
     * directory denoted by the given relative directory path.
     * <p>
     * There is no guarantee that the name strings in the resulting array
     * will appear in any specific order; they are not, in particular,
     * guaranteed to appear in alphabetical order.
     *
     * @param path The relative directory path.
     *
     * @return An array of strings naming the files and directories in the
     * directory denoted by the given relative directory path.
     * The array will be empty if the directory is empty.
     *
     * @throws IOException If the directory given by the relative path does not exists.
     */
    public abstract String[] list(String path) throws IOException;

    public abstract boolean exists(String path);

    /**
     * Returns an array of strings naming the relative directory path
     * of all file names.
     * <p>
     * There is no guarantee that the name strings in the resulting array
     * will appear in any specific order; they are not, in particular,
     * guaranteed to appear in alphabetical order.
     *
     * @return An array of strings naming the relative directory path
     * and filename to all files.
     * The array will be empty if the directory is empty.
     *
     * @throws IOException If an I/O error is thrown when accessing the virtual dir.
     */
    public abstract String[] listAllFiles() throws IOException;

    /**
     * Closes access to this virtual directory.
     */
    public abstract void close();

    /**
     * Creates an instance of a virtual directory object from a given directory or ZIP-file.
     *
     * @param file A directory or a ZIP-file.
     *
     * @return The virtual directory instance, or {@code null} if {@code file} is not a directory or a ZIP-file or
     * the ZIP-file could not be opened for read access..
     */
    public static VirtualDir create(File file) {
            if (file.isDirectory()) {
                return new Dir(file);
            }
            try {
                return new Zip(new ZipFile(file));
            } catch (IOException ignored) {
                return null;
            }
        }
    /*
     The replacement for usage with the NIO implementation has been disabled,
     because some Sentinel 1 product result in problems in the zipfs code.
     This maybe lrelates to BigZIP but not all zip files bigger 2 GB have problems

    public static VirtualDir create(File file) {
        String basePath = file.getPath();
        boolean isZip;
        URI vdURI;
        try {
            URI uri = file.toURI();
            if (file.isDirectory()) {
                isZip = false;
                vdURI = uri;
            } else if (file.getName().toLowerCase().endsWith(".zip")) {
                vdURI = ensureZipURI(uri);
                isZip = true;
            } else {
                return null;
            }
            Path virtualDirPath = getPathFromURI(vdURI);
            return new NIO(virtualDirPath, basePath, isZip, isZip, isZip);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static URI ensureZipURI(URI uri) throws IOException {
        Path basePath = getPathFromURI(uri);
        String baseUri = uri.toString();
        if (baseUri.startsWith("file:") && basePath.toFile().isFile()) {
            uri = URI.create("jar:" + baseUri + "!/");
        }
        return uri;
    }

    static Path getPathFromURI(URI uri) throws IOException {
        // Must synchronize, because otherwise FS could have been created by concurrent thread
        synchronized (VirtualDir.class) {
            try {
                return Paths.get(uri);
            } catch (FileSystemNotFoundException exp) {
                Map<String, String> env = Collections.emptyMap();
                FileSystems.newFileSystem(uri, env, null);
                return Paths.get(uri);
            }
        }
    }
    */

    public abstract boolean isCompressed();

    public abstract boolean isArchive();

    public File getTempDir() throws IOException {
        return null;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private static class Dir extends VirtualDir {

        private final File dir;

        private Dir(File file) {
            dir = file;
        }

        @Override
        public String getBasePath() {
            return dir.getPath();
        }

        @Override
        public InputStream getInputStream(String path) throws IOException {
            if (path.endsWith(".gz")) {
                return new GZIPInputStream(new BufferedInputStream(new FileInputStream(getFile(path))));
            }
            return new BufferedInputStream(new FileInputStream(getFile(path)));
        }

        @Override
        public File getFile(String path) throws IOException {
            File child = new File(dir, path);
            if (!child.exists()) {
                throw new FileNotFoundException(child.getPath());
            }
            return child;
        }

        @Override
        public String[] list(String path) throws IOException {
            File child = getFile(path);
            return child.list();
        }

        @Override
        public boolean exists(String path) {
            File child = new File(dir, path);
            return child.exists();
        }

        @Override
        public String[] listAllFiles() throws IOException {
            Path path = Paths.get(dir.getAbsolutePath());
            try (Stream<Path> pathStream = Files.walk(path)) {
                Stream<Path> filteredstream = pathStream.filter(new Predicate<Path>() {
                    @Override
                    public boolean test(Path path) {
                        return Files.isRegularFile(path);
                    }
                });
                final int baseLength = path.toUri().toString().length();
                Stream<String> fileStream = filteredstream.map(new Function<Path, String>() {
                    @Override
                    public String apply(Path path) {
                        return path.toUri().toString().substring(baseLength);
                    }
                });
                return fileStream.toArray(new IntFunction<String[]>() {
                    @Override
                    public String[] apply(int value) {
                        return new String[value];
                    }
                });
            }
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isCompressed() {
            return false;
        }

        @Override
        public boolean isArchive() {
            return false;
        }
    }

    private static class Zip extends VirtualDir {

        private static final int BUFFER_SIZE = 4 * 1024 * 1024;

        private ZipFile zipFile;
        private File tempZipFileDir;

        private Zip(ZipFile zipFile) {
            this.zipFile = zipFile;
        }

        @Override
        public String getBasePath() {
            return zipFile.getName();
        }

        @Override
        public InputStream getInputStream(String path) throws IOException {
            return getInputStream(getEntry(path));
        }

        @Override
        public File getFile(String path) throws IOException {

            ZipEntry zipEntry = getEntry(path);

            if (tempZipFileDir == null) {
                tempZipFileDir = VirtualDir.createUniqueTempDir();
            }

            File tempFile = new File(tempZipFileDir, zipEntry.getName());
            if (tempFile.exists()) {
                return tempFile;
            }

            // System.out.println("Extracting ZIP-entry to " + tempFile);
            if (zipEntry.isDirectory()) {
                tempFile.mkdirs();
            } else {
                unzip(zipEntry, tempFile);
            }

            return tempFile;
        }

        @Override
        public String[] list(String path) throws IOException {
            if (".".equals(path) || path.isEmpty()) {
                path = "";
            } else if (!path.endsWith("/")) {
                path += "/";
            }
            boolean dirSeen = false;
            TreeSet<String> nameSet = new TreeSet<>();
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String name = zipEntry.getName();
                if (name.startsWith(path)) {
                    int i1 = path.length();
                    int i2 = name.indexOf('/', i1);
                    String entryName;
                    if (i2 == -1) {
                        entryName = name.substring(i1);
                    } else {
                        entryName = name.substring(i1, i2);
                    }
                    if (!entryName.isEmpty() && !nameSet.contains(entryName)) {
                        nameSet.add(entryName);
                    }
                    dirSeen = true;
                }
            }
            if (!dirSeen) {
                throw new FileNotFoundException(getBasePath() + "!" + path);
            }
            return nameSet.toArray(new String[nameSet.size()]);
        }

        @Override
        public boolean exists(String path) {
            try {
                ZipEntry zipEntry = getEntry(path);
                return zipEntry != null;
            } catch (FileNotFoundException e) {
                return false;
            }
        }

        @Override
        public String[] listAllFiles() {
            List<String> filenameList = new ArrayList<>();
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                if (!zipEntry.isDirectory()) {
                    filenameList.add(zipEntry.getName());
                }
            }
            return filenameList.toArray(new String[0]);
        }

        @Override
        public void close() {
            cleanup();
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            cleanup();
        }

        @Override
        public boolean isCompressed() {
            return true;
        }

        @Override
        public boolean isArchive() {
            return true;
        }

        @Override
        public File getTempDir() throws IOException {
            return tempZipFileDir;
        }

        private void cleanup() {
            try {
                zipFile.close();
                zipFile = null;
            } catch (IOException ignored) {
                // ok
            }
            if (tempZipFileDir != null) {
                deleteFileTree(tempZipFileDir);
            }
        }

        private InputStream getInputStream(ZipEntry zipEntry) throws IOException {
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            if (zipEntry.getName().endsWith(".gz")) {
                return new GZIPInputStream(inputStream);
            }
            return inputStream;
        }

        private ZipEntry getEntry(String path) throws FileNotFoundException {
            ZipEntry zipEntry = zipFile.getEntry(path);
            if (zipEntry == null) {
                throw new FileNotFoundException(zipFile.getName() + "!" + path);
            }
            return zipEntry;
        }

        private void unzip(ZipEntry zipEntry, File tempFile) throws IOException {
            try (InputStream is = zipFile.getInputStream(zipEntry)) {
                if (is != null) {
                    tempFile.getParentFile().mkdirs();
                    try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(tempFile), BUFFER_SIZE)) {
                        byte[] bytes = new byte[1024];
                        int n;
                        while ((n = is.read(bytes)) > 0) {
                            os.write(bytes, 0, n);
                        }
                    } catch (IOException ioe) {
                        throw new IOException("Failed to unzip '" + zipEntry.getName() + "'to '" + tempFile.getAbsolutePath() + "'", ioe);
                    }
                }
            }
        }
    }

    private static class NIO extends VirtualDir {

        private final Path virtualDirPath;
        private final String basePath;
        private final boolean isCompressed;
        private final boolean isArchive;
        private final boolean localCopyRequired;
        private Path tempZipFilePathStore;

        private NIO(Path virtualDirPath, String basePath, boolean isCompressed, boolean isArchive, boolean localCopyRequired) {
            this.virtualDirPath = virtualDirPath;
            this.basePath = basePath;
            this.isCompressed = isCompressed;
            this.isArchive = isArchive;
            this.localCopyRequired = localCopyRequired;
        }

        @Override
        public String getBasePath() {
            return basePath;
        }

        @Override
        public InputStream getInputStream(String path) throws IOException {
            Path resolve = virtualDirPath.resolve(path);
            if (Files.exists(resolve)) {
                InputStream inputStream = Files.newInputStream(resolve);
                if (path.endsWith(".gz")) {
                    return new GZIPInputStream(new BufferedInputStream(inputStream));
                }
                return new BufferedInputStream(inputStream);
            } else {
                throw new FileNotFoundException(resolve.toString());
            }
        }

        @Override
        public synchronized File getFile(String path) throws IOException {
            Path resolve = virtualDirPath.resolve   (path);
            if (!Files.exists(resolve)) {
                throw new FileNotFoundException(resolve.toString());
            }

            if (localCopyRequired) {
                if (tempZipFilePathStore == null) {
                    tempZipFilePathStore = VirtualDir.createUniqueTempDir().toPath();
                }
                Path tempFilePath = tempZipFilePathStore.resolve(path);
                if (Files.notExists(tempFilePath)) {
                    if(Files.notExists(tempFilePath.getParent())){
                        Files.createDirectories(tempFilePath.getParent());
                    }
                    Files.copy(resolve, tempFilePath);
                }
                return tempFilePath.toFile();
            } else {
                return resolve.toFile();
            }
        }


        @Override
        public String[] list(String path) throws IOException {
            Path startingPath = virtualDirPath.resolve(path);
            if (!Files.exists(startingPath)) {
                throw new FileNotFoundException(startingPath.toString());
            }
            List<String> fileNames = new ArrayList<>();
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(startingPath)) {
                for (Path child : directoryStream) {
                    String filename = child.getFileName().toString();
                    if (filename.endsWith("/") && filename.length() > 0) {
                        filename = filename.substring(0, filename.length() - 1);
                    }
                    fileNames.add(filename);
                }
            }
            return fileNames.toArray(new String[fileNames.size()]);
        }

        @Override
        public String[] listAllFiles() throws IOException {
            try (Stream<Path> pathStream = Files.walk(virtualDirPath)) {
                Stream<Path> filteredstream = pathStream.filter(new Predicate<Path>() {
                    @Override
                    public boolean test(Path path) {
                        return Files.isRegularFile(path);
                    }
                });
                final int baseLength = virtualDirPath.toUri().toString().length();
                Stream<String> fileStream = filteredstream.map(new Function<Path, String>() {
                    @Override
                    public String apply(Path path) {
                        return path.toUri().toString().substring(baseLength);
                    }
                });
                return fileStream.toArray(new IntFunction<String[]>() {
                    @Override
                    public String[] apply(int value) {
                        return new String[value];
                    }
                });
            }
        }

        @Override
        public boolean exists(String path) {
            return Files.exists(virtualDirPath.resolve(path));
        }

        @Override
        public void close() {

        }

        @Override
        public boolean isCompressed() {
            return isCompressed;
        }

        @Override
        public boolean isArchive() {
            return isArchive;
        }

        @Override
        public File getTempDir() throws IOException {
            return tempZipFilePathStore.toFile();
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            cleanup();
        }

        private void cleanup() {
            if (tempZipFilePathStore != null) {
                deleteFileTree(tempZipFilePathStore.toFile());
            }
        }
    }

    /**
     * Deletes the directory <code>treeRoot</code> and all the content recursively.
     *
     * @param treeRoot directory to be deleted
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void deleteFileTree(File treeRoot) {
        Assert.notNull(treeRoot);

        File[] files = treeRoot.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFileTree(file);
                } else {
                    file.delete();
                }
            }
        }
        treeRoot.delete();
    }

    private static final int TEMP_DIR_ATTEMPTS = 10000;

    public static File createUniqueTempDir() throws IOException {
        File baseDir = getBaseTempDir();
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                return tempDir;
            }
        }
        throw new IllegalStateException("Failed to create directory within "
                                        + TEMP_DIR_ATTEMPTS + " attempts (tried "
                                        + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
    }

    private static File getBaseTempDir() throws IOException {
        String contextId = "snap";
        File tempDir;
        String tempDirName = System.getProperty("java.io.tmpdir");
        if (tempDirName != null) {
            tempDir = new File(tempDirName);
            if (tempDir.exists()) {
                String userName = System.getProperty("user.name");
                tempDir = new File(tempDir, contextId + "-" + userName);
                tempDir.mkdir();
            }
        } else {
            tempDir = new File(System.getProperty("user.home", "."), "." + contextId + "/temp");
            tempDir.mkdirs();
        }
        if (!tempDir.exists()) {
            throw new IOException("Temporary directory not available: " + tempDir);
        }
        return tempDir;
    }

}
