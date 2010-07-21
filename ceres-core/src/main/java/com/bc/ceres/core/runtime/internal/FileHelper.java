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

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CanceledException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import static com.bc.ceres.core.runtime.Constants.MODULE_MANIFEST_NAME;

import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileHelper {
    // Important, place only all lower case strings here
    private static final String[][] NATIVE_MAPPINGS = new String[][]{
            {"lib/win/", "windows"},
            {"lib/macosx/", "mac os x"},
            {"lib/linux/", "linux"},
            {"lib/solaris/", "solaris"},
            {"lib/aix/", "aix"},
            {"lib/hpux/", "hp ux"},
    };

    public static URI urlToUri(URL url) throws URISyntaxException {
        return new URI(url.toExternalForm().replace(" ", "%20"));
    }

    public static File urlToFile(URL url) {
        try {
            if ("jar".equalsIgnoreCase(url.getProtocol())) {
                String path = url.getPath();
                int jarEntrySepPos = path.lastIndexOf("!/");
                if (jarEntrySepPos > 0) {
                    path = path.substring(0, jarEntrySepPos);
                }
                url = new URL(path);
            }
            URI uri = urlToUri(url);
            // Exhaustive checking on uri required to prevent
            // File.File(URI) constructor from throwing an IllegalArgumentException
            if ("file".equalsIgnoreCase(uri.getScheme())
                    && uri.isAbsolute()
                    && !uri.isOpaque()
                    && uri.getAuthority() == null
                    && uri.getFragment() == null
                    && uri.getQuery() == null) {
                return new File(uri);
            }
        } catch (MalformedURLException e) {
            // ignored
        } catch (URISyntaxException e) {
            // ignored
        }
        return null;
    }

    public static URL fileToUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static URL locationToManifestUrl(URL locationUrl) {
        String location = locationUrl.toExternalForm();

        String xmlUrlString;
        if (JarFilenameFilter.isJarName(location)) {
            xmlUrlString = "jar:" + location + "!/" + MODULE_MANIFEST_NAME;
        } else if (location.endsWith("/")) {
            xmlUrlString = location + MODULE_MANIFEST_NAME;
        } else {
            return null;
        }

        try {
            return new URL(xmlUrlString);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static URL manifestToLocationUrl(URL manifestUrl) {
        String location = manifestUrl.toExternalForm();
        if (!location.endsWith(MODULE_MANIFEST_NAME)) {
            return null;
        }
        location = location.substring(0, location.length() - MODULE_MANIFEST_NAME.length());
        location = location.replace(" ", "%20");  // fixes bug in maven surefire plugin

        // A JAR URL?
        String prefix = "jar:";
        String suffix = "!/";
        if (location.startsWith(prefix) && location.endsWith(suffix)) {
            location = location.substring(prefix.length(), location.length() - suffix.length());
        }

        try {
            return new URL(location);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

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
        InputStream inputStream = new FileInputStream(source);
        try {
            OutputStream outputStream = new FileOutputStream(target);
            try {
                String taskName = MessageFormat.format("Copying {0}", source.getName());
                int fileSize = (int) Math.max(source.length(), (long) Integer.MAX_VALUE);
                copy(inputStream, outputStream, taskName, fileSize, pm);
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
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

        ZipFile zipFile = new ZipFile(sourceZipFile);
        pm.beginTask(MessageFormat.format("Unpacking {0} to {1}", sourceZipFile.getName(), targetDir.getName()),
                     zipFile.size());
        try {
            ArrayList<String> entries = new ArrayList<String>(zipFile.size());
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            String osName = System.getProperty("os.name");
            String osNameLC = osName != null ? osName.toLowerCase() : "";
            while (enumeration.hasMoreElements()) {
                checkIOTaskCanceled(pm);
                ZipEntry zipEntry = enumeration.nextElement();
                String entryName = zipEntry.getName();
                boolean include = true;
                if (isNative) {
                    String osIndicator = getOsIndicator(entryName);
                    if (osIndicator != null) {
                        include = osNameLC.indexOf(osIndicator) >= 0;
                    }
                }

                // Leave here for debugging
                // System.out.println("entryName = " + entryName + " (" + (include ? "include" : "exclude") + ")");

                if (include) {
                    pm.setSubTaskName(entryName);
                    File targetFile = new File(targetDir, entryName);
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

    private static String getOsIndicator(String entryName) {
        String entryNameLC = entryName.toLowerCase();
        for (String[] nativeMapping : NATIVE_MAPPINGS) {
            if (entryNameLC.startsWith(nativeMapping[0])) {
                return nativeMapping[1];
            }
        }
        return null;
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
                FileInputStream inputStream = new FileInputStream(sourceFile);
                try {
                    zipOutputStream.putNextEntry(zipEntry);
                    copy(inputStream, zipOutputStream, entryName, (int) sourceFile.length(),
                         SubProgressMonitor.create(pm, 1));
                    zipOutputStream.closeEntry();
                    entries.add(entryName);
                } finally {
                    inputStream.close();
                }
            }
            return entries;
        } finally {
            pm.done();
            zipOutputStream.close();
        }
    }
}
