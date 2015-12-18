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

package org.esa.snap.dataio.netcdf.util;

import org.esa.snap.core.util.SystemUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.hdf4.H4iosp;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.nc2.iosp.hdf5.H5iosp;
import ucar.nc2.iosp.netcdf3.N3raf;
import ucar.nc2.util.DiskCache;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.UncompressInputStream;
import ucar.unidata.io.bzip2.CBZip2InputStream;
import ucar.unidata.util.StringUtil2;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Helper class to open Netcdf files.
 * Compared to the {@link ucar.nc2.NetcdfFile#open(String)} the process of opening the file is simplified.
 * Only nc3, nc4, hdf4 and hdf5 are supported which speeds up the opening process.
 */
public class NetcdfFileOpener {

    private static final Logger LOG = SystemUtils.LOG;

    private static final int MAGIC_BUFFER_LENGTH = 8;
    private static final int COPY_BUFFER_LENGTH = 100000;
    private static final byte[] NC3_MAGIC = {0x43, 0x44, 0x46, 0x01};
    private static final byte[] NC3_MAGIC_LONG = {0x43, 0x44, 0x46, 0x02}; // 64-bit offset format : only affects the variable offset value
    private static final byte[] H4_MAGIC = {0x0e, 0x03, 0x13, 0x01};
    private static final byte[] H5_MAGIC = {(byte) 0x89, 'H', 'D', 'F', '\r', '\n', 0x1a, '\n'};

// currently unused
//    public static boolean canOpenNetcdf(Object input) throws IOException {
//        ucar.unidata.io.RandomAccessFile raf = null;
//        try {
//            raf = getRaf(input);
//            return (raf != null) ? canOpen(raf) : false;
//        } finally {
//            if (raf != null) {
//                raf.close();
//            }
//        }
//    }
//
//    private static boolean canOpen(ucar.unidata.io.RandomAccessFile raf) throws IOException {
//        for (IOServiceProvider iosp : IOSPs) {
//            if (iosp.isValidFile(raf)) {
//                return true;
//            }
//        }
//        return false;
//    }


    public static NetcdfFile open(Object input) throws IOException {
        try {
            byte[] buffer = new byte[MAGIC_BUFFER_LENGTH];
            readMagicBytes(input, buffer);
            IOServiceProvider spi = getIOSpi(buffer);
            if (spi != null) {
                RandomAccessFile raf = getRaf(input);
                if (raf != null) {
                    return new DummyNetcdfFile(spi, raf, raf.getLocation());
                }
            } else {
                // The HDF5 superblock, which begins with the 8-byte format signature,
                // may begin at certain predefined offsets within the HDF5 file:
                // 0, 512, 1024, 2048, and multiples of two thereafter.
                // see: http://www.hdfgroup.org/HDF5/doc/H5.format.html
                RandomAccessFile rafForTesting = getRafForTesting(input);
                boolean isValidHdf5 = H5header.isValidFile(rafForTesting);
                if (isValidHdf5) {
                    RandomAccessFile raf = getRaf(input);
                    if (raf != null) {
                        return new DummyNetcdfFile(new H5iosp(), raf, raf.getLocation());
                    }
                } else {
                    if (!(input instanceof ImageInputStream)) {
                        rafForTesting.close();
                    }
                }
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable t) {
            throw new IOException("Error while opening netcdf file", t);
        }
        return null;
    }

    private static File getFile(Object input) {
        if (input instanceof String) {
            String inputString = (String) input;
            String filePrefix = "file:";
            if (inputString.startsWith(filePrefix)) {
                inputString = inputString.substring(filePrefix.length());
            }
            return new File(inputString);
        } else if (input instanceof File) {
            return (File) input;
        }
        throw new IllegalArgumentException();
    }

    private static void readMagicBytes(Object input, byte[] buffer) throws IOException {
        if (input instanceof String || input instanceof File) {
            readMagicBytesFromFile(getFile(input), buffer);
        } else if (input instanceof ImageInputStream) {
            ImageInputStream imageInputStream = (ImageInputStream) input;
            imageInputStream.seek(0);
            imageInputStream.read(buffer);
            // do not close this stream, it's an argument
        }
    }

    private static RandomAccessFile getRafForTesting(Object input) throws IOException {
        if (input instanceof String || input instanceof File) {
            File file = getFile(input);
            MemoryCacheImageInputStream imageInputStream = new MemoryCacheImageInputStream(openInputStream(file));
            return new ImageInputStreamRandomAccessFile(imageInputStream, file.length());
        } else if (input instanceof ImageInputStream) {
            ImageInputStream imageInputStream = (ImageInputStream) input;
            return new ImageInputStreamRandomAccessFile(imageInputStream, imageInputStream.length());
        }
        throw new IllegalArgumentException();
    }

    private static InputStream openInputStream(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        if (fileName.endsWith(".z")) {
            is = new UncompressInputStream(is);
        } else if (fileName.endsWith(".zip")) {
            ZipInputStream zin = new ZipInputStream(is);
            ZipEntry ze = zin.getNextEntry();
            if (ze != null) {
                is = zin;
            }
        } else if (fileName.endsWith(".bz2")) {
            is = new CBZip2InputStream(is, true);
        } else if (fileName.endsWith(".gzip") || fileName.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        return is;
    }

    private static void readMagicBytesFromFile(File file, byte[] buffer) throws IOException {
        try (InputStream iss = openInputStream(file)) {
            iss.read(buffer);
        }
    }

    private static IOServiceProvider getIOSpi(byte[] buffer) {
        IOServiceProvider spi = null;
        if (testMagicBytes(buffer, NC3_MAGIC)) {
            spi = new N3raf();
        } else if (testMagicBytes(buffer, NC3_MAGIC_LONG)) {
            spi = new N3raf();
        } else if (testMagicBytes(buffer, H4_MAGIC)) {
            spi = new H4iosp();
        } else if (testMagicBytes(buffer, H5_MAGIC)) {
            spi = new H5iosp();
        }
        return spi;
    }

    private static boolean testMagicBytes(byte[] buffer, byte[] magic) {
        for (int i = 0; i < magic.length; i++) {
            if (buffer[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static ucar.unidata.io.RandomAccessFile getRaf(Object input) throws IOException {
        if (input instanceof String) {
            String location = (String) input;
            return getRafFromFile(location);
        } else if (input instanceof File) {
            File file = (File) input;
            return getRafFromFile(file.getAbsolutePath());
        } else if (input instanceof ImageInputStream) {
            ImageInputStream imageInputStream = (ImageInputStream) input;
            return new ImageInputStreamRandomAccessFile(imageInputStream, imageInputStream.length());
        }
        return null;
    }

    // copied from ucar.nc2.NetcdfFile
    private static ucar.unidata.io.RandomAccessFile getRafFromFile(String location) throws IOException {

        String uriString = location.trim();

        ucar.unidata.io.RandomAccessFile raf;
// Currently those protocols are not supported by BEAM
//        if (uriString.startsWith("http:")) { // open through URL
//            raf = new ucar.unidata.io.http.HTTPRandomAccessFile(uriString);
//
//        } else if (uriString.startsWith("nodods:")) { // open through URL
//            uriString = "http" + uriString.substring(6);
//            raf = new ucar.unidata.io.http.HTTPRandomAccessFile(uriString);
//
//        } else if (uriString.startsWith("slurp:")) { // open through URL
//            uriString = "http" + uriString.substring(5);
//            byte[] contents = IO.readURLContentsToByteArray(uriString); // read all into memory
//            raf = new InMemoryRandomAccessFile(uriString, contents);
//
//        } else {
        // get rid of crappy microsnot \ replace with happy /
        uriString = StringUtil2.replace(uriString, '\\', "/");

        if (uriString.startsWith("file:")) {
            // uriString = uriString.substring(5);
            uriString = StringUtil2.unescape(uriString.substring(5));  // 11/10/2010 from erussell@ngs.org
        }

        String uncompressedFileName = null;
        try {
            uncompressedFileName = makeUncompressed(uriString);
        } catch (Exception e) {
            LOG.warning("Failed to uncompress " + uriString + " err= " + e.getMessage() + "; try as a regular file.");
            //allow to fall through to open the "compressed" file directly - may be a misnamed suffix
        }

        if (uncompressedFileName != null) {
            // open uncompressed file as a RandomAccessFile.
            raf = new ucar.unidata.io.RandomAccessFile(uncompressedFileName, "r");
            //raf = new ucar.unidata.io.MMapRandomAccessFile(uncompressedFileName, "r");

        } else {
            // normal case - not compressed
            raf = new ucar.unidata.io.RandomAccessFile(uriString, "r");
            //raf = new ucar.unidata.io.MMapRandomAccessFile(uriString, "r");
        }
//        }

        return raf;
    }

    // copied from ucar.nc2.NetcdfFile
    static private String makeUncompressed(String filename) throws Exception {
        // see if its a compressed file
        int pos = filename.lastIndexOf('.');
        if (pos < 0) {
            return null;
        }

        String suffix = filename.substring(pos + 1);
        String uncompressedFilename = filename.substring(0, pos);

        if (!suffix.equalsIgnoreCase("Z") && !suffix.equalsIgnoreCase("zip") && !suffix.equalsIgnoreCase("gzip")
            && !suffix.equalsIgnoreCase("gz") && !suffix.equalsIgnoreCase("bz2")) {
            return null;
        }

        // see if already decompressed, look in cache if need be
        File uncompressedFile = DiskCache.getFileStandardPolicy(uncompressedFilename);
        if (uncompressedFile.exists() && uncompressedFile.length() > 0) {
            // see if its locked - another thread is writing it
            FileInputStream stream = null;
            FileLock lock = null;
            try {
                stream = new FileInputStream(uncompressedFile);
                // obtain the lock
                while (true) { // loop waiting for the lock
                    try {
                        lock = stream.getChannel().lock(0, 1, true); // wait till its unlocked
                        break;

                    } catch (OverlappingFileLockException oe) { // not sure why lock() doesnt block
                        try {
                            Thread.sleep(100); // msecs
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                }
                LOG.fine("found uncompressed " + uncompressedFile + " for " + filename);
                return uncompressedFile.getPath();
            } finally {
                if (lock != null) {
                    lock.release();
                }
                if (stream != null) {
                    stream.close();
                }
            }
        }

        // ok gonna write it
        // make sure compressed file exists
        File file = new File(filename);
        if (!file.exists()) {
            return null; // bail out  */
        }

        FileOutputStream fout = new FileOutputStream(uncompressedFile);

        // obtain the lock
        FileLock lock = null;
        while (true) { // loop waiting for the lock
            try {
                lock = fout.getChannel().lock(0, 1, false);
                break;

            } catch (OverlappingFileLockException oe) { // not sure why lock() doesnt block
                try {
                    Thread.sleep(100); // msecs
                } catch (InterruptedException ignored) {
                }
            }
        }

        try {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(filename), COPY_BUFFER_LENGTH);
            if (suffix.equalsIgnoreCase("Z")) {
                try (InputStream in = new UncompressInputStream(inputStream)) {
                    copy(in, fout, COPY_BUFFER_LENGTH);
                }
                LOG.fine("uncompressed " + filename + " to " + uncompressedFile);
            } else if (suffix.equalsIgnoreCase("zip")) {
                ZipInputStream zin = new ZipInputStream(inputStream);
                ZipEntry ze = zin.getNextEntry();
                if (ze != null) {
                    copy(zin, fout, COPY_BUFFER_LENGTH);
                    LOG.fine("unzipped " + filename + " entry " + ze.getName() + " to " + uncompressedFile);
                }
            } else if (suffix.equalsIgnoreCase("bz2")) {
                try (InputStream in = new CBZip2InputStream(inputStream, true)) {
                    copy(in, fout, COPY_BUFFER_LENGTH);
                }
                LOG.fine("unbzipped " + filename + " to " + uncompressedFile);
            } else if (suffix.equalsIgnoreCase("gzip") || suffix.equalsIgnoreCase("gz")) {
                try (InputStream in = new GZIPInputStream(inputStream)) {
                    copy(in, fout, COPY_BUFFER_LENGTH);
                }
                LOG.fine("ungzipped " + filename + " to " + uncompressedFile);
            }
        } catch (Exception e) {

            // appears we have to close before we can delete
            fout.close();
            fout = null;

            // dont leave bad files around
            if (uncompressedFile.exists()) {
                if (!uncompressedFile.delete()) {
                    LOG.warning("failed to delete uncompressed file (IOException)" + uncompressedFile);
                }
            }
            throw e;

        } finally {
            if (lock != null) {
                lock.release();
            }
            if (fout != null) {
                fout.close();
            }
        }

        return uncompressedFile.getPath();
    }

    // copied from ucar.nc2.NetcdfFile
    static private void copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) {
                break;
            }
            out.write(buffer, 0, bytesRead);
        }
    }

    // use internal class to defer execution of static initializer
    private static class DummyNetcdfFile extends NetcdfFile {

        private DummyNetcdfFile(IOServiceProvider spi, RandomAccessFile raf, String location) throws IOException {
            super(spi, raf, location, null);
        }
    }

    private static class ImageInputStreamRandomAccessFile extends RandomAccessFile {

        private final ImageInputStream imageInputStream;
        private final long length;

        public ImageInputStreamRandomAccessFile(ImageInputStream imageInputStream, long length) {
            super(16000);
            this.imageInputStream = imageInputStream;
            this.length = length;
        }

        @Override
        public String getLocation() {
            return "ImageInputStream";
        }

        @Override
        public long length() throws IOException {
            return length;
        }

        @Override
        protected int read_(long pos, byte[] b, int offset, int len) throws IOException {
            imageInputStream.seek(pos);
            return imageInputStream.read(b, offset, len);
        }

        @Override
        public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
            int n = (int) nbytes;
            byte[] buff = new byte[n];
            int done = read_(offset, buff, 0, n);
            dest.write(ByteBuffer.wrap(buff));
            return done;
        }

        @Override
        public void close() throws IOException {
            imageInputStream.close();
        }
    }
}
