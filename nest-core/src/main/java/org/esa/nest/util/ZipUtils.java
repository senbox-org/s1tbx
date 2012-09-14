package org.esa.nest.util;

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import org.esa.beam.util.io.FileUtils;

import java.io.*;

/**
 * For zipping and unzipping compressed files
 */
public class ZipUtils {

    private final static String[] extList = { ".zip", ".gz", ".z", ".7z" };

    public static boolean isZipped(final File file) {
        final String name = file.getName().toLowerCase();
        for(String ext : extList) {
            if(name.endsWith(ext))
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
            inArchive.extract(in, false,  extractCB);

            return extractCB.getTargetFiles();
        } finally {
            if (inArchive != null) {
                inArchive.close();
            }
            randomAccessFile.close();
        }
    }

    public static ISequentialOutStream unzipToStream(final File file) throws Exception {

        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        ISevenZipInArchive inArchive = null;
        try {
            inArchive = SevenZip.openInArchive(null, // autodetect archive type
                    new RandomAccessFileInStream(randomAccessFile));

            final int numItems = inArchive.getNumberOfItems();
            final int[] in = new int[numItems];
            for (int i = 0; i < in.length; i++) {
                in[i] = i;
            }
            final ExtractCallback extractCB = new ExtractCallback(inArchive, file, null);
            inArchive.extract(in, false, // Non-test mode
                    extractCB);

            ISequentialOutStream stream = extractCB.getStream(0, ExtractAskMode.EXTRACT);
            return stream;
        } finally {
            if (inArchive != null) {
                inArchive.close();
            }
            randomAccessFile.close();
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
                            if(path == null) {
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
