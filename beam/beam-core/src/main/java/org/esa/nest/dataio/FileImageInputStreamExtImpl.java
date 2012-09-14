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

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;

/**
 * An implementation of {@link ImageInputStream} that gets its input from a
 * {@link File}. The eraf contents are assumed to be stable during the lifetime
 * of the object.
 * 
 * @author Simone Giannecchini, GeoSolutions
 */
public final class FileImageInputStreamExtImpl extends ImageInputStreamImpl
        implements ImageInputStream {

    /** the associated {@link File}*/
    protected File file;

    protected EnhancedRandomAccessFile eraf;

    private boolean isClosed = false;

    public static ImageInputStream createInputStream(final File file) throws IOException {
        return new FileImageInputStreamExtImpl(file);
        //return new FileImageInputStream(file);
    }

    @Override
    public byte readByte() throws IOException {

        return eraf.readByte();
    }

    @Override
    public char readChar() throws IOException {

        return eraf.readChar();
    }

    @Override
    public double readDouble() throws IOException {

        return eraf.readDouble();
    }

    @Override
    public float readFloat() throws IOException {

        return eraf.readFloat();
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {

        eraf.readFully(b, off, len);
    }

    @Override
    public void readFully(byte[] b) throws IOException {

        eraf.readFully(b);
    }

    @Override
    public int readInt() throws IOException {

        return eraf.readInt();
    }

    @Override
    public String readLine() throws IOException {

        return eraf.readLine();
    }

    @Override
    public ByteOrder getByteOrder() {

        return eraf.getByteOrder();
    }

    @Override
    public long getStreamPosition() throws IOException {
        return eraf.getFilePointer();
    }

    @Override
    public boolean isCached() {
        return eraf.isCached();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return eraf.read(b);
    }

    @Override
    public long skipBytes(long n) throws IOException {

        return eraf.skipBytes(n);
    }

    @Override
    public long readLong() throws IOException {

        return eraf.readLong();
    }

    @Override
    public short readShort() throws IOException {

        return eraf.readShort();
    }

    @Override
    public int readUnsignedByte() throws IOException {

        return eraf.readUnsignedByte();
    }

    @Override
    public long readUnsignedInt() throws IOException {

        return eraf.readUnsignedInt();
    }

    @Override
    public int readUnsignedShort() throws IOException {

        return eraf.readUnsignedShort();
    }

    @Override
    public String readUTF() throws IOException {

        return eraf.readUTF();
    }

    @Override
    public void setByteOrder(ByteOrder byteOrder) {

        eraf.setByteOrder(byteOrder);
        super.setByteOrder(byteOrder);
    }

    @Override
    public int skipBytes(int n) throws IOException {

        return eraf.skipBytes(n);
    }

    /**
     * Constructs a {@link FileImageInputStreamExtImpl} that will read from a
     * given {@link File}.
     * 
     * <p>
     * The eraf contents must not change between the time this object is
     * constructed and the time of the last call to a read method.
     * 
     * @param f
     *                a {@link File} to read from.
     * 
     * @exception NullPointerException
     *                    if <code>f</code> is <code>null</code>.
     * @exception SecurityException
     *                    if a security manager exists and does not allow read
     *                    access to the eraf.
     * @exception FileNotFoundException
     *                    if <code>f</code> is a directory or cannot be opened
     *                    for reading for any other reason.
     * @exception IOException
     *                    if an I/O error occurs.
     */
    public FileImageInputStreamExtImpl(File f) throws FileNotFoundException,
            IOException {
        this(f, -1);
    }

    /**
     * Constructs a {@link FileImageInputStreamExtImpl} that will read from a
     * given {@link File}.
     * 
     * <p>
     * The eraf contents must not change between the time this object is
     * constructed and the time of the last call to a read method.
     * 
     * @param f
     *                a {@link File} to read from.
     * @param bufferSize
     *                size of the underlying buffer.
     * 
     * @exception NullPointerException
     *                    if <code>f</code> is <code>null</code>.
     * @exception SecurityException
     *                    if a security manager exists and does not allow read
     *                    access to the eraf.
     * @exception FileNotFoundException
     *                    if <code>f</code> is a directory or cannot be opened
     *                    for reading for any other reason.
     * @exception IOException
     *                    if an I/O error occurs.
     */
    public FileImageInputStreamExtImpl(File f, int bufferSize) throws IOException {
        // //
        //
        // Check that the input file is a valid file
        //
        // //
        if (f == null) {
            throw new NullPointerException("f == null!");
        }
        final StringBuilder buff = new StringBuilder(
                "Invalid input file provided");
        if (!f.exists() || f.isDirectory()) {
            buff.append("exists: ").append(f.exists()).append("\n");
            buff.append("isDirectory: ").append(f.isDirectory()).append("\n");
            throw new FileNotFoundException(buff.toString());
        }
        if (!f.exists() || f.isDirectory() || !f.canRead()) {
            buff.append("canRead: ").append(f.canRead()).append("\n");
            throw new IOException(buff.toString());
        }
        this.file = f;
        this.eraf = bufferSize <= 0 ? new EnhancedRandomAccessFile(f, "r")
                : new EnhancedRandomAccessFile(f, "r", bufferSize);
        // NOTE: this must be done accordingly to what ImageInputStreamImpl
        // does, otherwise some ImageReader subclasses might not work.
        this.eraf.setByteOrder(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads an int from the underlying {@link EnhancedRandomAccessFile}.
     */
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

    /**
     * Read up to <code>len</code> bytes into an array, at a specified offset.
     * This will block until at least one byte has been read.
     * 
     * @param b
     *                the byte array to receive the bytes.
     * @param off
     *                the offset in the array where copying will start.
     * @param len
     *                the number of bytes to copy.
     * @return the actual number of bytes read, or -1 if there is not more data
     *         due to the end of the eraf being reached.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        //checkClosed();
        bitOffset = 0;
        int nbytes = eraf.readBytes(b, off, len);
        if (nbytes != -1) {
            streamPos += nbytes;
        }
        return nbytes;
    }

    /**
     * Returns the length of the underlying eraf, or <code>-1</code> if it is
     * unknown.
     * 
     * @return the eraf length as a <code>long</code>, or <code>-1</code>.
     */
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
     * Seeks the current position to pos.
     */
    @Override
    public void seek(long pos) throws IOException {
        //checkClosed();
        //if (pos < flushedPos) {
        //    throw new IllegalArgumentException("pos < flushedPos!");
        //}
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
     * Provides a simple description for this {@link ImageInputStream}.
     * 
     * @return a simple description for this {@link ImageInputStream}.
     */
    @Override
    public String toString() {

        return new StringBuilder("FileImageInputStreamExtImpl which points to ")
                .append(this.file.toString()).toString();
    }
}
