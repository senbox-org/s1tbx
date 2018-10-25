/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.util;

import org.esa.snap.core.util.SystemUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * For zipping and unzipping compressed files
 */
public class ZipUtils {

    private final static String[] extList = {".zip", ".gz", ".z"};

    public static boolean isZipped(final File file) {
        final String name = file.getName().toLowerCase();
        for (String ext : extList) {
            if (name.endsWith(ext))
                return true;
        }
        return false;
    }

    public static boolean isZip(final File inputFile) {
        return inputFile.getName().toLowerCase().endsWith(".zip");
    }

    public static boolean findInZip(final File file, final String prefix, final String suffix) {
        try {
            final ZipFile productZip = new ZipFile(file, ZipFile.OPEN_READ);

            final Optional result = productZip.stream()
                    .filter(ze -> !ze.isDirectory())
                    .filter(ze -> ze.getName().toLowerCase().endsWith(suffix))
                    .filter(ze -> ze.getName().toLowerCase().startsWith(prefix))
                    .findFirst();
            return result.isPresent();
        } catch (Exception e) {
            SystemUtils.LOG.warning("unable to read zip file " + file + ": " + e.getMessage());
        }
        return false;
    }

    public static String getRootFolder(final File file, final String headerFileName) throws IOException {
        final ZipFile productZip = new ZipFile(file, ZipFile.OPEN_READ);

        final Optional result = productZip.stream()
                .filter(ze -> !ze.isDirectory())
                .filter(ze -> ze.getName().toLowerCase().endsWith(headerFileName))
                .findFirst();
        if (result.isPresent()) {
            ZipEntry ze = (ZipEntry) result.get();
            String path = ze.toString();
            int sepIndex = path.lastIndexOf('/');
            if (sepIndex > 0) {
                return path.substring(0, sepIndex) + '/';
            } else {
                return "";
            }
        }
        return "";
    }

    public static boolean isValid(final File file) {
        try (ZipFile zipfile = new ZipFile(file)) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
                ZipEntry ze = zis.getNextEntry();
                if (ze == null) {
                    return false;
                }
                while (ze != null) {
                    // if it throws an exception fetching any of the following then we know the file is corrupted.
                    try (InputStream in = zipfile.getInputStream(ze)) {
                        ze.getCrc();
                        ze.getCompressedSize();
                        ze = zis.getNextEntry();
                    } catch (IOException e) {
                        return false;
                    }
                }
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    public static void unzip(Path sourceFile, Path destination, boolean keepFolderStructure) throws IOException {
        if (sourceFile == null || destination == null) {
            throw new IllegalArgumentException("One of the arguments is null");
        }
        if (!Files.exists(destination)) {
            Files.createDirectory(destination);
        }
        byte[] buffer;
        try (ZipFile zipFile = new ZipFile(sourceFile.toFile())) {
            ZipEntry entry;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (entry.isDirectory() && !keepFolderStructure)
                    continue;
                Path filePath = destination.resolve(entry.getName());
                Path strippedFilePath = destination.resolve(filePath.getFileName());
                if (!Files.exists(filePath)) {
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        try (InputStream inputStream = zipFile.getInputStream(entry)) {
                            try (BufferedOutputStream bos = new BufferedOutputStream(
                                    new FileOutputStream(keepFolderStructure ? filePath.toFile() : strippedFilePath.toFile()))) {
                                buffer = new byte[4096];
                                int read;
                                while ((read = inputStream.read(buffer)) > 0) {
                                    bos.write(buffer, 0, read);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void zipFile(final File file, final File outputZipFile) throws IOException {

        try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(outputZipFile))) {
            addToZipFile(file, zipStream);
        }
    }

    private static void addToZipFile(File file, ZipOutputStream zipStream) {

        try (FileInputStream inputStream = new FileInputStream(file.getPath())) {

            ZipEntry entry = new ZipEntry(file.getName());
            entry.setCreationTime(FileTime.fromMillis(file.lastModified()));
            zipStream.putNextEntry(entry);

            final byte[] readBuffer = new byte[2048];
            int amountRead;
            int written = 0;

            while ((amountRead = inputStream.read(readBuffer)) > 0) {
                zipStream.write(readBuffer, 0, amountRead);
                written += amountRead;
            }
        } catch (Exception e) {
            SystemUtils.LOG.severe("Unable to zip " + file);
        }
    }
}
