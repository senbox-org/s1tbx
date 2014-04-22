/*
 *    ImageI/O-Ext - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *    https://imageio-ext.dev.java.net/
 *    (C) 2007 - 2008, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.esa.nest.dataio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageOutputStreamImpl;

/**
 * An implementation of {@link ImageOutputStream} that take its output on a
 * {@link File}.
 * 
 * @author Daniele Romagnoli, GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public final class FileImageOutputStreamExtImpl extends ImageOutputStreamImpl
        implements ImageOutputStream {

    protected EnhancedRandomAccessFile eraf;

    protected File file;
    private boolean isClosed = false;

    public static ImageOutputStream createOutputStream(final File file) throws IOException {
        return new FileImageOutputStreamExtImpl(file);
        //return new FileImageOutputStream(file);
    }

    /**
     * A constructor which accepts a File as input.
     * 
     * @param file eraf
     *
     * @throws IOException
     */
    public FileImageOutputStreamExtImpl(File file)
            throws IOException {
        this.file = file;
        eraf = new EnhancedRandomAccessFile(file, "rw");
        // NOTE: this must be done accordingly to what ImageInputStreamImpl
        // does, otherwise some ImageREader subclasses might not work.
        this.eraf.setByteOrder(ByteOrder.BIG_ENDIAN);

    }

    /**
     * A constructor which accepts a File as input.
     * 
     * @param file eraf
     * @param bufSize
     *
     * @throws IOException
     */
    public FileImageOutputStreamExtImpl(File file, int bufSize)
            throws IOException {
        this.file = file;
        eraf = new EnhancedRandomAccessFile(file, "rw", bufSize);
        // NOTE: this must be done accordingly to what ImageInputStreamImpl
        // does, otherwise some ImageREader subclasses might not work.
        this.eraf.setByteOrder(ByteOrder.BIG_ENDIAN);

    }

    @Override
    public int read() throws IOException {
        //checkClosed();
        bitOffset = 0;
        int val = eraf.read();
        if (val != -1) {
            ++streamPos;
        }
        return val;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        //checkClosed();
        bitOffset = 0;
        int nbytes = eraf.read(b, off, len);
        if (nbytes != -1) {
            streamPos += nbytes;
        }
        return nbytes;
    }

    @Override
    public void write(int b) throws IOException {
        //checkClosed();
        flushBits();
        eraf.write(b);
        ++streamPos;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        //checkClosed();
        flushBits();
        eraf.writeBytes(b, off, len);
        streamPos += len;
    }

    @Override
    public long length() {
        try {
            //checkClosed();
            return eraf.length();
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * Sets the current stream position and resets the bit offset to 0. It is
     * legal to seeking past the end of the eraf; an <code>EOFException</code>
     * will be thrown only if a read is performed. The eraf length will not be
     * increased until a write is performed.
     * 
     * @exception IndexOutOfBoundsException
     *                    if <code>pos</code> is smaller than the flushed
     *                    position.
     * @exception IOException
     *                    if any other I/O error occurs.
     */
    @Override
    public void seek(long pos) throws IOException {
        //checkClosed();
        if (pos < flushedPos) {
            throw new IndexOutOfBoundsException("IndexOutOfBounds pos < flushedPos! " + pos + " < "+ flushedPos);
        }
        bitOffset = 0;
        streamPos = eraf.seek(pos);
    }

    /**
     * Closes the underlying {@link EnhancedRandomAccessFile}.
     * 
     * @throws IOException
     *                 in case something bad happens.
     */
    @Override
    public void close() throws IOException {
    	try{
	    	if(!isClosed){
		        super.close();
		        eraf.close();
	    	}
    	}
    	finally{
    		isClosed=true;
    	}
    }

    /**
     * Retrieves the {@link File} we are connected to.
     */
    public File getFile() {
        return file;
    }

    /**
     * Disposes this {@link FileImageInputStreamExtImpl} by closing its
     * underlying {@link EnhancedRandomAccessFile}.
     * 
     */
    public void dispose() {
        try {
            close();
        } catch (IOException e) {
            //
        }
    }

    /**
     * Provides a simple description for this {@link ImageOutputStream}.
     * 
     * @return a simple description for this {@link ImageOutputStream}.
     */
    @Override
    public String toString() {

        return new StringBuilder("FileImageOutputStreamExtImpl which points to ")
                .append(this.file.toString()).toString();
    }
}
