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

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CanceledException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class IOHelper {

    public static String getBaseName(File archiveFile) {
        String dirName = archiveFile.getName();
        int pos = dirName.lastIndexOf((int) '.');
        if (pos > 1) {
            dirName = dirName.substring(0, pos);
        }
        return dirName;
    }

    public static String getFileName(URL url) throws UnsupportedEncodingException {
        return new File(URLDecoder.decode(url.getFile(), "UTF-8")).getName();
    }

    public static void copy(File source, File target, ProgressMonitor pm) throws IOException, CanceledException {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(source));
             OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(target))) {
            String taskName = MessageFormat.format("Copying {0}", source.getName());
            int fileSize = (int) Math.max(source.length(), (long) Integer.MAX_VALUE);
            copy(inputStream, outputStream, taskName, fileSize, pm);
        }
    }

    public static void copy(InputStream inputStream, OutputStream outputStream, String taskName, int fileSize,
                            ProgressMonitor pm) throws IOException, CanceledException {
        pm.beginTask(taskName, fileSize);
        try {
            byte[] buffer = new byte[64 * 1024];
            while (true) {
                checkIOTaskCanceled(pm);
                int n = inputStream.read(buffer);
                if (n > 0) {
                    outputStream.write(buffer, 0, n);
                    pm.worked(n);
                } else if (n < buffer.length) {
                    break;
                }
            }
        } finally {
            pm.done();
        }
    }

    private static void checkIOTaskCanceled(ProgressMonitor pm) throws CanceledException {
        if (pm.isCanceled()) {
            throw new CanceledException();
        }
    }

    public static void createDirectory(File dir) throws IOException {
        if (dir.exists()) {
            return;
        }
        if (!dir.mkdir()) {
            throw new IOException(MessageFormat.format("Failed to create directory ''{0}''", dir));
        }
    }

    public static void copy(URLConnection source, File target, ProgressMonitor pm) throws IOException, CanceledException {
        InputStream inputStream = source.getInputStream();
        try {
            OutputStream outputStream = new FileOutputStream(target);
            try {
                String taskName = MessageFormat.format("Downloading {0}", target.getName());
                int contentLength = source.getContentLength();
                copy(inputStream, outputStream, taskName, contentLength, pm);
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
    }

    public static void copy(ZipFile zipFile, ZipEntry source, File target, ProgressMonitor pm) throws IOException, CanceledException {
        InputStream inputStream = zipFile.getInputStream(source);
        try {
            OutputStream outputStream = new FileOutputStream(target);
            try {
                String taskName = MessageFormat.format("Unpacking {0}", source.getName());
                int fileSize = (int) source.getSize();
                copy(inputStream, outputStream, taskName, fileSize, pm);
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
    }

    public static List<String> unpack(File sourceZipFile, File targetDir, boolean isNative, ProgressMonitor pm) throws IOException, CanceledException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        Logger logger = Logger.getLogger(System.getProperty("ceres.context", "ceres"));
        final Platform platform = isNative ? Platform.getCurrentPlatform() : null;
        if (platform != null) {
            logger.info(MessageFormat.format("Archive [{0}]: Detected platform {1}{2}",
                                             sourceZipFile, platform.getId(), platform.getBitCount()));
        }

        ZipFile zipFile = new ZipFile(sourceZipFile);
        pm.beginTask(MessageFormat.format("Unpacking {0} to {1}", sourceZipFile.getName(), targetDir.getName()),
                     zipFile.size());
        try {
            ArrayList<String> entries = new ArrayList<String>(zipFile.size());
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

            while (enumeration.hasMoreElements()) {
                checkIOTaskCanceled(pm);
                final ZipEntry zipEntry = enumeration.nextElement();
                final String entryName = zipEntry.getName();
                final File targetFile;

                if (platform != null && Platform.isAnyPlatformDir(entryName)) {
                    if (platform.isPlatformDir(entryName)) {
                        targetFile = new File(targetDir, platform.truncatePlatformDir(entryName));
                        logger.info(MessageFormat.format("Archive [{0}]: Unpacking platform dependent entry [{1}]",
                                                         sourceZipFile, entryName));
                    } else {
                        targetFile = null;
                        logger.fine(MessageFormat.format("Archive [{0}]: Ignoring platform dependent entry [{1}]",
                                                         sourceZipFile, entryName));
                    }
                } else {
                    targetFile = new File(targetDir, entryName);
                }

                if (targetFile != null) {
                    pm.setSubTaskName(entryName);
                    File parentDir = targetFile.getParentFile();
                    if (parentDir != null) {
                        parentDir.mkdirs();
                    }
                    if (zipEntry.isDirectory()) {
                        targetFile.mkdir();
                        pm.worked(1);
                    } else {
                        copy(zipFile, zipEntry, targetFile, SubProgressMonitor.create(pm, 1));
                    }
                    entries.add(entryName);
                }
            }
            return entries;
        } finally {
            pm.done();
            zipFile.close();
        }
    }

    public static List<String> pack(File sourceDir, File targetZipFile, ProgressMonitor pm) throws IOException, CanceledException {
        if (!sourceDir.exists()) {
            throw new FileNotFoundException(sourceDir.getPath());
        }

        DirScanner dirScanner = new DirScanner(sourceDir, true, true);
        String[] entryNames = dirScanner.scan();
        ZipOutputStream zipOutputStream = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(targetZipFile)));
        zipOutputStream.setMethod(ZipEntry.DEFLATED);
        pm.beginTask(MessageFormat.format("Packing {0} into {1}", sourceDir.getName(), targetZipFile.getName()),
                     entryNames.length);

        ArrayList<String> entries = new ArrayList<String>(entryNames.length);
        try {
            for (String entryName : entryNames) {
                checkIOTaskCanceled(pm);
                ZipEntry zipEntry = new ZipEntry(entryName.replace('\\', '/'));

                pm.setSubTaskName(entryName);
                File sourceFile = new File(sourceDir, entryName);
                try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile))) {
                    zipOutputStream.putNextEntry(zipEntry);
                    copy(inputStream, zipOutputStream, entryName, (int) sourceFile.length(),
                         SubProgressMonitor.create(pm, 1));
                    zipOutputStream.closeEntry();
                    entries.add(entryName);
                }
            }
            return entries;
        } finally {
            pm.done();
            zipOutputStream.close();
        }
    }

}
