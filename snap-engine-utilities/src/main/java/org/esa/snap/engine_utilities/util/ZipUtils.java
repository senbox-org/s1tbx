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

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * For zipping and unzipping compressed files
 */
public class ZipUtils {

    private final static String[] extList = {".zip", ".gz", ".z", ".7z"};

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
        if(result.isPresent()) {
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

    // 7zip

    public static File[] unzipToFolder(final File inFile, final File outFolder) throws Exception {

        ISevenZipInArchive inArchive = null;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(inFile, "r")) {
            inArchive = SevenZip.openInArchive(null, // autodetect archive type
                                               new RandomAccessFileInStream(randomAccessFile));

            final int[] in = new int[inArchive.getNumberOfItems()];
            for (int i = 0; i < in.length; i++) {
                in[i] = i;
            }
            final ExtractCallback extractCB = new ExtractCallback(inArchive, inFile, outFolder);
            inArchive.extract(in, false, extractCB);

            return extractCB.getTargetFiles();
        } finally {
            if (inArchive != null) {
                inArchive.close();
            }

        }
    }

    public static InputStream unZipToStream(final File file) throws Exception {
        return new ZipArchiveInputStream(new FileInputStream(file));
    }

    public static void zipFolder(final Path directory, final File outputZipFile) throws IOException {

        try( ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(outputZipFile)) ) {

            // traverse every file in the selected directory and add them
            // to the zip file by calling addToZipFile(..)
            DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory);
            dirStream.forEach(path -> addToZipFile(path.toFile(), zipStream));
        } catch (IOException e) {
            throw e;
        }
    }

    public static void zipFile(final File file, final File outputZipFile) throws IOException {

        try( ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(outputZipFile)) ) {
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
            SystemUtils.LOG.severe("Unable to zip "+file);
        }
    }

    public static InputStream unzipToStream(final File file) throws Exception {

        ISevenZipInArchive inArchive = null;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            inArchive = SevenZip.openInArchive(null, // autodetect archive type
                                               new RandomAccessFileInStream(randomAccessFile));

            final int numItems = inArchive.getNumberOfItems();

            ISimpleInArchiveItem item = inArchive.getSimpleInterface().getArchiveItem(0);
            return new ArchiveInputStreamHandler(item).getInputStream();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inArchive != null) {
                inArchive.close();
            }

        }
        return null;
    }


    private static class ArchiveInputStreamHandler {

        private final ISimpleInArchiveItem item;
        private ByteArrayInputStream arrayInputStream;

        public ArchiveInputStreamHandler(final ISimpleInArchiveItem item) {
            this.item = item;
        }

        public InputStream getInputStream() throws SevenZipException {

            item.extractSlow(new ISequentialOutStream() {
                @Override
                public int write(byte[] data) throws SevenZipException {
                    arrayInputStream = new ByteArrayInputStream(data);
                    return data.length; // Return amount of consumed data
                }
            });
            return arrayInputStream;
        }
    }

    private static class ExtractCallback implements IArchiveExtractCallback {
        private int index;
        private int current = -1;
        private boolean skipExtraction;
        private final ISevenZipInArchive inArchive;
        private final File inFile;
        private final File outFolder;
        private OutputStream out;
        private final File[] targetFiles;

        public ExtractCallback(final ISevenZipInArchive inArchive, final File inFile, final File outFolder) throws SevenZipException {
            this.inArchive = inArchive;
            this.inFile = inFile;
            this.outFolder = outFolder;
            this.targetFiles = new File[inArchive.getNumberOfItems()];
        }

        public File[] getTargetFiles() {
            return targetFiles;
        }

        public ISequentialOutStream getStream(final int index, final ExtractAskMode extractAskMode)
                throws SevenZipException {
            this.index = index;
            skipExtraction = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);
            if (skipExtraction || extractAskMode != ExtractAskMode.EXTRACT) {
                return null;
            }
            return new ISequentialOutStream() {
                public int write(byte[] data) throws SevenZipException {
                    try {
                        if (index != current) {
                            if (out != null) {
                                out.flush();
                                out.close();
                            }
                            current = index;

                            Object path = inArchive.getProperty(index, PropID.PATH);
                            if (path == null) {
                                path = FileUtils.getFilenameWithoutExtension(inFile);
                            }
                            final File target = new File(outFolder, String.valueOf(path));
                            targetFiles[index] = target;
                            if(!target.getParentFile().exists()) {
                                if (!target.getParentFile().mkdirs()) {
                                    SystemUtils.LOG.severe("Unable to create folders in " + target.getParentFile());
                                }
                            }
                            out = new BufferedOutputStream(new FileOutputStream(target));
                        }
                        try {
                            out.write(data, 0, data.length);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        return data.length;
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }

        public void prepareOperation(final ExtractAskMode extractAskMode) throws SevenZipException {
        }

        public void setOperationResult(final ExtractOperationResult extractOperationResult) throws SevenZipException {
            if (skipExtraction) {
                return;
            }
            try {
                out.flush();
                out.close();
            } catch (Exception ex) {
                //
            }

            if (extractOperationResult != ExtractOperationResult.OK) {
                //System.err.println("Extraction error");
            } else {
                //System.out.println(String.format("%9X | %s",
                //        hash, inArchive.getProperty(index, PropID.PATH)));
            }
        }

        public void setCompleted(final long completeValue) throws SevenZipException {
        }

        public void setTotal(final long total) throws SevenZipException {
        }

    }
}
