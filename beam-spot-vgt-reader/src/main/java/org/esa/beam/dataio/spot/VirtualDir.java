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

package org.esa.beam.dataio.spot;

import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A read-only directory that can either be a directory in the file system or a ZIP-file.
 *
 * @author Norman Fomferra
 * @since BEAM 4.8
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
     * <p/>
     * There is no guarantee that the name strings in the resulting array
     * will appear in any specific order; they are not, in particular,
     * guaranteed to appear in alphabetical order.
     *
     * @param path The relative directory path.
     *
     * @return An array of strings naming the files and directories in the
     *         directory denoted by the given relative directory path.
     *         The array will be empty if the directory is empty.
     *
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
     *
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
                tempZipFileDir = new File(getTempDir(),
                                          FileUtils.getFilenameWithoutExtension(new File(zipFile.getName())));
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
            boolean dirSeen = false;
            ArrayList<String> names = new ArrayList<String>(32);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String name = zipEntry.getName();
                if (name.startsWith(path)) {
                    String entryName = name.substring(path.length() + ("".equals(path) ? 0 : 1));
                    if (!entryName.isEmpty()) {
                        names.add(entryName);
                    }
                    dirSeen = true;
                }
            }
            if (!dirSeen) {
                throw new FileNotFoundException(getBasePath() + "!" + path);
            }
            return names.toArray(new String[names.size()]);
        }

        @Override
        public void close() {
            try {
                zipFile.close();
            } catch (IOException e) {
                // ok
            }
            if (tempZipFileDir != null) {
                SystemUtils.deleteFileTree(tempZipFileDir);
            }
        }

        private InputStream getInputStream(ZipEntry zipEntry) throws IOException {
            return zipFile.getInputStream(zipEntry);
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
                tempDir = new File(SystemUtils.getUserHomeDir(), ".beam/temp");
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
}
