/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import java.io.*;
import java.util.Enumeration;
import java.util.TreeSet;
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
     * @return A reader for the specified relative path.
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
     * @return An input stream for the specified relative path.
     * @throws IOException If the file does not exist or if it can't be opened for reading.
     */
    public abstract InputStream getInputStream(String path) throws IOException;

    /**
     * Gets the file for the given relative path.
     *
     * @param path The relative file or directory path.
     * @return Gets the file or directory for the specified file path.
     * @throws IOException If the file or directory does not exist or if it can't be extracted from a ZIP-file.
     */
    public abstract File getFile(String path) throws IOException;

    /**
     * Returns an array of strings naming the files and directories in the
     * directory denoted by the given relative directory path.
     * <p/>
     * There is no guarantee that the name strings in the resulting array
     * will appear in any specific order; they are not, in particular,
     * guaranteed to appear in alphabetical order.
     *
     * @param path The relative directory path.
     * @return An array of strings naming the files and directories in the
     *         directory denoted by the given relative directory path.
     *         The array will be empty if the directory is empty.
     * @throws IOException If the directory given by the relative path does not exists.
     */
    public abstract String[] list(String path) throws IOException;

    /**
     * Closes access to this virtual directory.
     */
    public abstract void close();

    /**
     * Creates an instance of a virtual directory object from a given directory or ZIP-file.
     *
     * @param file A directory or a ZIP-file.
     * @return The virtual directory instance, or {@code null} if {@code file} is not a directory or a ZIP-file or
     *         the ZIP-file could not be opened for read access..
     */
    public static VirtualDir create(File file) {
        if (file.isDirectory()) {
            return new Dir(file);
        }
        try {
            return new Zip(new ZipFile(file));
        } catch (IOException e) {
            return null;
        }
    }

    public abstract boolean isCompressed();

    public abstract boolean isArchive();

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
                return new GZIPInputStream(new FileInputStream(getFile(path)));
            }
            return new FileInputStream(getFile(path));
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

        private final ZipFile zipFile;
        private File tempZipFileDir;

        private Zip(ZipFile zipFile) throws IOException {
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
                tempZipFileDir = new File(getTempDir(), getFilenameWithoutExtensionFromPath(getBasePath()));
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
            if (path.equals(".") || path.isEmpty()) {
                path = "";
            } else if (!path.endsWith("/")) {
                path += "/";
            }
            boolean dirSeen = false;
            TreeSet<String> nameSet = new TreeSet<String>();
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String name = zipEntry.getName();
                if (name.startsWith(path)) {
                    int i1 = path.length();
                    int i2 = name.indexOf("/", i1);
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
        public void close() {
            try {
                zipFile.close();
            } catch (IOException e) {
                // ok
            }
            if (tempZipFileDir != null) {
                deleteFileTree(tempZipFileDir);
            }
        }

        @Override
        public boolean isCompressed() {
            return true;
        }

        @Override
        public boolean isArchive() {
            return true;
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

        private static File getTempDir() throws IOException {
            File tempDir = null;
            String tempDirName = System.getProperty("java.io.tmpdir");
            if (tempDirName != null) {
                tempDir = new File(tempDirName);
            }
            if (tempDir == null || !tempDir.exists()) {
                tempDir = new File(new File(System.getProperty("user.home", ".")), ".beam/temp");
                tempDir.mkdirs();
            }
            if (!tempDir.exists()) {
                throw new IOException("Temporary directory not available: " + tempDir);
            }
            return tempDir;
        }

        private void unzip(ZipEntry zipEntry, File tempFile) throws IOException {
            InputStream is = zipFile.getInputStream(zipEntry);
            if (is != null) {
                try {
                    tempFile.getParentFile().mkdirs();
                    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(tempFile), BUFFER_SIZE);
                    try {
                        byte[] bytes = new byte[1024];
                        int n;
                        while ((n = is.read(bytes)) > 0) {
                            os.write(bytes, 0, n);
                        }
                    } finally {
                        os.close();
                    }
                } finally {
                    is.close();
                }
            }
        }
    }

    /**
     * Gets the filename without its extension from the given filename.
     *
     * @param path the path of the file whose filename is to be extracted.
     * @return the filename without its extension.
     */
    private static String getFilenameWithoutExtensionFromPath(String path) {
        String fileName = getFileNameFromPath(path);
        int i = fileName.lastIndexOf('.');
        if (i > 0 && i < fileName.length() - 1) {
            return fileName.substring(0, i);
        }
        return fileName;
    }

    private static String getFileNameFromPath(String path) {
        String fileName;
        int lastChar = path.lastIndexOf(File.separator);
        if (lastChar >= 0) {
            fileName = path.substring(lastChar + 1, path.length());
        } else {
            fileName = path;
        }
        return fileName;
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
}
