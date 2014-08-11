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
package org.esa.snap.util;

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.esa.beam.util.io.FileUtils;

import java.io.*;

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

    public static File[] unzipToFolder(final File inFile, final File outFolder) throws Exception {

        final RandomAccessFile randomAccessFile = new RandomAccessFile(inFile, "r");
        ISevenZipInArchive inArchive = null;
        try {
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
            randomAccessFile.close();
        }
    }

    public static InputStream unZipToStream(final File file) throws Exception {
        return new ZipArchiveInputStream(new FileInputStream(file));
    }

    public static InputStream unzipToStream(final File file) throws Exception {

        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        ISevenZipInArchive inArchive = null;
        try {
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
            randomAccessFile.close();
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
                            target.getParentFile().mkdirs();
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
