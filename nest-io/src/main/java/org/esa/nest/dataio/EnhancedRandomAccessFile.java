/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UTFDataFormatException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * @author Simone Giannecchini, GeoSolutions.
 * 
 * -- NOTES from a class we derived this class from --
 * 
 * 
 * RandomAccessFile.java. By Russ Rew, based on BufferedRandomAccessFile by Alex
 * McManus, based on Sun's source code for java.io.RandomAccessFile. For Alex
 * McManus version from which this derives, see his <a
 * href="http://www.aber.ac.uk/~agm/Java.html"> Freeware Java Classes</a>.
 * 
 * A buffered drop-in replacement for java.io.RandomAccessFile. Instances of
 * this class realise substantial speed increases over java.io.RandomAccessFile
 * through the use of buffering. This is a subclass of Object, as it was not
 * possible to subclass java.io.RandomAccessFile because many of the methods are
 * final. However, if it is necessary to use RandomAccessFile and
 * java.io.RandomAccessFile interchangeably, both classes implement the
 * DataInput and DataOutput interfaces.
 * 
 * @author Alex McManus
 * @author Russ Rew
 * @author john caron
 * 
 * @version $Id: EnhancedRandomAccessFile.java,v 1.2 2011-08-17 19:18:57 lveci Exp $
 * @see DataInput
 * @see DataOutput
 * @see java.io.RandomAccessFile
 * @todo optimize {@link #readLine()}
 * @task {@link ByteOrder} is not respected with writing
 */
public final class EnhancedRandomAccessFile implements DataInput, DataOutput {

	/** _more_ */
	static public final int BIG_ENDIAN = 0;

	/** _more_ */
	static public final int LITTLE_ENDIAN = 1;

	// debug leaks - keep track of open files

	/** The default buffer size, in bytes. */
	public static final int DEFAULT_BUFFER_SIZE = 32768;

	/** _more_ */
	protected File file;

	/** The underlying java.io.RandomAccessFile. */
	protected java.io.RandomAccessFile eraf;

	/**
	 * The offset in bytes from the eraf start, of the next read or write
	 * operation.
	 */
	protected long filePosition;

	/** The buffer used to load the data. */
	protected byte buffer[];

	/**
	 * The offset in bytes of the start of the buffer, from the start of the
	 * eraf.
	 */
	protected long bufferStart;

	/**
	 * The offset in bytes of the end of the data in the buffer, from the start
	 * of the eraf. This can be calculated from
	 * <code>bufferStart + dataSize</code>, but it is cached to speed up the
	 * read( ) method.
	 */
	protected long dataEnd;

	/**
	 * The size of the data stored in the buffer, in bytes. This may be less
	 * than the size of the buffer.
	 */
	protected int dataSize;

	/** True if we are at the end of the eraf. */
	protected boolean endOfFile;

	/** The access mode of the eraf. */
	protected boolean readonly;

	/** The current endian (big or little) mode of the eraf. */
	protected boolean bigEndian;

	/** True if the data in the buffer has been modified. */
	boolean bufferModified = false;

	/** make sure eraf is this long when closed */
	protected long minLength = 0;

	/**
	 * _more_
	 * 
	 * @param bufferSize
	 *            _more_
	 */
	protected EnhancedRandomAccessFile(int bufferSize) {
		eraf = null;
		readonly = true;
		init(bufferSize);
	}

	/**
	 * Constructor, default buffer size.
	 * 
	 * @param file
	 *            file of the eraf
	 * @param mode
	 *            same as for java.io.RandomAccessFile
	 * @throws IOException
	 */
	public EnhancedRandomAccessFile(File file, String mode)
			throws IOException {
		this(file, mode, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Constructor.
	 * 
	 * @param file
	 *            file of the eraf
	 * @param mode
	 *            same as for java.io.RandomAccessFile
	 * @param bufferSize
	 *            size of buffer to use.
	 * @throws IOException
	 */
	public EnhancedRandomAccessFile(File file, String mode, int bufferSize)
			throws IOException {
		this.file = file;
		this.eraf = new java.io.RandomAccessFile(file, mode);
		this.readonly = mode.equals("r");
		init(bufferSize);

	}

	public java.io.RandomAccessFile getRandomAccessFile() {
		return this.eraf;
	}

	/**
	 * _more_
	 * 
	 * @param bufferSize
	 *            _more_
	 */
	private void init(int bufferSize) {
		// Initialise the buffer
		bufferStart = 0;
		dataEnd = 0;
		dataSize = 0;
		filePosition = 0;
		buffer = new byte[bufferSize];
		endOfFile = false;
	}

	/**
	 * Close the eraf, and release any associated system resources.
	 * 
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public void close() throws IOException {

		if (eraf == null) {
			return;
		}

		// If we are writing and the buffer has been modified, flush the
		// contents of the buffer.
		if (!readonly && bufferModified) {
			eraf.seek(bufferStart);
			eraf.write(buffer, 0, dataSize);
		}

		// may need to extend eraf, in case no fill is neing used
		// may need to truncate eraf in case overwriting a longer eraf
		// use only if minLength is set (by N3iosp)
		if (!readonly && (minLength != 0) && (minLength != eraf.length())) {
			eraf.setLength(minLength);
			// System.out.println("TRUNCATE!!! minlength="+minLength);
		}

		// Close the underlying eraf object.
		eraf.close();

	}

	/**
	 * Close silently the underlying {@link RandomAccessFile}
	 * 
	 * 
	 */
	public void finalize() {
		try {
			close();
		} catch (IOException ex) {
		}
	}

	/**
	 * Return true if eraf pointer is at end of eraf.
	 * 
	 * @return _more_
	 */
	public boolean isAtEndOfFile() {
		return endOfFile;
	}

	// Create channel from eraf

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public FileChannel getChannel() {
		if (eraf == null) {
			return null;
		}

		try {
			eraf.seek(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return eraf.getChannel();
	}

	/**
	 * Set the position in the eraf for the next read or write.
	 * 
	 * @param pos
	 *            the offset (in bytes) from the start of the eraf.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public long seek(long pos) throws IOException {

		// If the seek is into the buffer, just update the eraf pointer.
		if ((pos >= bufferStart) && (pos < dataEnd)) {
			filePosition = pos;
			return filePosition;
		}

		// If the current buffer is modified, write it to disk.
		if (bufferModified) {
			flush();
		}

		// need new buffer
		bufferStart = pos;
		filePosition = pos;

        eraf.seek(pos);
        //if(readonly)
        //    dataSize = eraf.read(buffer, 0, buffer.length);
        //else
        //    dataSize = eraf.read(buffer, 0, 1);
        if(readonly)
            dataSize = read_(pos, buffer, 0, buffer.length);
        else
            dataSize = read_(pos, buffer, 0, 1);

        if (dataSize <= 0) {
			dataSize = 0;
			endOfFile = true;
		} else {
			endOfFile = false;
		}

		// Cache the position of the buffer end.
		dataEnd = bufferStart + dataSize;
        return filePosition;
    }

	/**
	 * Returns the current position in the eraf, where the next read or write
	 * will occur.
	 * 
	 * @return the offset from the start of the eraf in bytes.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public long getFilePointer() throws IOException {
		return filePosition;
	}

	/**
	 * Get the eraf file, or name.
	 * 
	 * @return _more_
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Get the length of the eraf. The data in the buffer (which may not have
	 * been written the disk yet) is taken into account.
	 * 
	 * @return the length of the eraf in bytes.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public long length() throws IOException {
		long fileLength = eraf.length();
		if (fileLength < dataEnd) {
			return dataEnd;
		} else {
			return fileLength;
		}
	}

	/**
	 * Change the current endian mode. Subsequent reads of short, int, float,
	 * double, long, char will use this. Does not currently effect writes.
	 * 
	 * @param bo
	 * 
	 */
	public void setByteOrder(final ByteOrder bo) {
		this.bigEndian = (bo == ByteOrder.BIG_ENDIAN);
	}

	public ByteOrder getByteOrder() {
		return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
	}

	/**
	 * Returns the opaque eraf descriptor object associated with this eraf.
	 * 
	 * @return the eraf descriptor object associated with this eraf.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public FileDescriptor getFD() throws IOException {
		return (eraf == null) ? null : eraf.getFD();
	}

	/**
	 * Copy the contents of the buffer to the disk.
	 * 
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public void flush() throws IOException {
		if (bufferModified) {
			eraf.seek(bufferStart);
			eraf.write(buffer, 0, dataSize);
			bufferModified = false;
		}

	}

	/**
	 * Make sure eraf is at least this long when its closed. needed when not
	 * using fill mode, and not all data is written.
	 * 
	 * @param minLength
	 *            _more_
	 */
	public void setMinLength(long minLength) {
		this.minLength = minLength;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////
	// Read primitives.
	//

	/**
	 * Read a byte of data from the eraf, blocking until data is available.
	 * 
	 * @return the next byte of data, or -1 if the end of the eraf is reached.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public int read() throws IOException {

		// If the eraf position is within the data, return the byte...
		if (filePosition < dataEnd) {
			final int pos = (int) (filePosition - bufferStart);
			filePosition++;
			return (buffer[pos] & 0xff);

			// ...or should we indicate EOF...
		} else if (endOfFile) {
			return -1;

			// ...or seek to fill the buffer, and try again.
		} else {
			seek(filePosition);
			return read();
		}
	}

	/**
	 * Read up to <code>len</code> bytes into an array, at a specified offset.
	 * This will block until at least one byte has been read.
	 * 
	 * @param b
	 *            the byte array to receive the bytes.
	 * @param off
	 *            the offset in the array where copying will start.
	 * @param len
	 *            the number of bytes to copy.
	 * @return the actual number of bytes read, or -1 if there is not more data
	 *         due to the end of the eraf being reached.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public int readBytes(byte b[], int off, int len) throws IOException {

		// Check for end of eraf.
		if (endOfFile) {
			return -1;
		}

		// See how many bytes are available in the buffer - if none,
		// seek to the eraf position to update the buffer and try again.
		final int bytesAvailable = (int) (dataEnd - filePosition);
		if (bytesAvailable < 1) {
			seek(filePosition);
			return readBytes(b, off, len);
		}

		// Copy as much as we can.
		final int copyLength = (bytesAvailable >= len) ? len : bytesAvailable;
		System.arraycopy(buffer, (int) (filePosition - bufferStart), b, off, copyLength);
		filePosition += copyLength;

		// If there is more to copy...
		if (copyLength < len) {
			int extraCopy = len - copyLength;

			// If the amount remaining is more than a buffer's length, read it
			// directly from the eraf.
			if (extraCopy > buffer.length) {
                eraf.seek(filePosition);
		        extraCopy = eraf.read(b, off + copyLength, len - copyLength);

				// ...or read a new buffer full, and copy as much as possible...
			} else {
				seek(filePosition);
				if (!endOfFile) {
					extraCopy = (extraCopy > dataSize) ? dataSize : extraCopy;
					System.arraycopy(buffer, 0, b, off + copyLength, extraCopy);
				} else {
					extraCopy = -1;
				}
			}

			// If we did manage to copy any more, update the eraf position and
			// return the amount copied.
			if (extraCopy > 0) {
				filePosition += extraCopy;
				return copyLength + extraCopy;
			}
		}

		// Return the amount copied.
		return copyLength;
	}

	/**
	 * read directly, without going through the buffer
	 * 
	 * @param pos
	 *            _more_
	 * @param b
	 *            _more_
	 * @param offset
	 *            _more_
	 * @param len
	 *            _more_
	 * 
	 * @return _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	private int read_(long pos, byte[] b, int offset, int len)
			throws IOException {

		eraf.seek(pos);
		return eraf.read(b, offset, len);
	}

	/**
	 * Read up to <code>len</code> bytes into an array, at a specified offset.
	 * This will block until at least one byte has been read.
	 * 
	 * @param b
	 *            the byte array to receive the bytes.
	 * @param off
	 *            the offset in the array where copying will start.
	 * @param len
	 *            the number of bytes to copy.
	 * @return the actual number of bytes read, or -1 if there is not more data
	 *         due to the end of the eraf being reached.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public int read(byte b[], int off, int len) throws IOException {
		return readBytes(b, off, len);
	}

	/**
	 * Read up to <code>b.length( )</code> bytes into an array. This will
	 * block until at least one byte has been read.
	 * 
	 * @param b
	 *            the byte array to receive the bytes.
	 * @return the actual number of bytes read, or -1 if there is not more data
	 *         due to the end of the eraf being reached.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public int read(byte b[]) throws IOException {
		return readBytes(b, 0, b.length);
	}

	/**
	 * Reads <code>b.length</code> bytes from this eraf into the byte array.
	 * This method reads repeatedly from the eraf until all the bytes are read.
	 * This method blocks until all the bytes are read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @param b
	 *            the buffer into which the data is read.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading all the bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void readFully(byte b[]) throws IOException {
		readFully(b, 0, b.length);
	}

	/**
	 * Reads exactly <code>len</code> bytes from this eraf into the byte
	 * array. This method reads repeatedly from the eraf until all the bytes are
	 * read. This method blocks until all the bytes are read, the end of the
	 * stream is detected, or an exception is thrown.
	 * 
	 * @param b
	 *            the buffer into which the data is read.
	 * @param off
	 *            the start offset of the data.
	 * @param len
	 *            the number of bytes to read.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading all the bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void readFully(byte b[], int off, int len) throws IOException {
		int n = 0;
		while (n < len) {
			final int count = this.read(b, off + n, len - n);
			if (count < 0) {
				throw new EOFException();
			}
			n += count;
		}
	}

	/**
	 * Skips exactly <code>n</code> bytes of input. This method blocks until
	 * all the bytes are skipped, the end of the stream is detected, or an
	 * exception is thrown.
	 * 
	 * @param n
	 *            the number of bytes to be skipped.
	 * @return the number of bytes skipped, which is always <code>n</code>.
	 * @exception EOFException
	 *                if this eraf reaches the end before skipping all the
	 *                bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public int skipBytes(int n) throws IOException {
		seek(filePosition + n);
		return n;
	}

	/**
	 * Skips exactly <code>n</code> bytes of input. This method blocks until
	 * all the bytes are skipped, the end of the stream is detected, or an
	 * exception is thrown.
	 * 
	 * @param n
	 *            the number of bytes to be skipped.
	 * @return the number of bytes skipped, which is always <code>n</code>.
	 * @exception EOFException
	 *                if this eraf reaches the end before skipping all the
	 *                bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public long skipBytes(long n) throws IOException {
		seek(filePosition + n);
		return n;
	}

	/**
	 * Unread the last byte read. This method should not be used more than once
	 * between reading operations, or strange things might happen.
	 */
	public void unread() {
		filePosition--;
	}

	//
	// Write primitives.
	//

	/**
	 * Write a byte to the eraf. If the eraf has not been opened for writing, an
	 * IOException will be raised only when an attempt is made to write the
	 * buffer to the eraf.
	 * <p>
	 * Caveat: the effects of seek( )ing beyond the end of the eraf are
	 * undefined.
	 * 
	 * 
	 * @param b
	 *            _more_
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public void write(int b) throws IOException {

		// If the eraf position is within the block of data...
		if (filePosition < dataEnd) {
			buffer[(int) (filePosition++ - bufferStart)] = (byte) b;
			bufferModified = true;

			// ...or (assuming that seek will not allow the eraf pointer
			// to move beyond the end of the eraf) get the correct block of
			// data...
		} else {

			// If there is room in the buffer, expand it...
			if (dataSize != buffer.length) {
				buffer[(int) (filePosition++ - bufferStart)] = (byte) b;
				bufferModified = true;
				dataSize++;
				dataEnd++;

				// ...or do another seek to get a new buffer, and start again...
			} else {
				seek(filePosition);
				write(b);
			}
		}
	}

	/**
	 * Write <code>len</code> bytes from an array to the eraf.
	 * 
	 * @param b
	 *            the array containing the data.
	 * @param off
	 *            the offset in the array to the data.
	 * @param len
	 *            the length of the data.
	 * @exception IOException
	 *                if an I/O error occurrs.
	 */
	public void writeBytes(byte b[], int off, int len) throws IOException {

		// If the amount of data is small (less than a full buffer)...
		if (len < buffer.length) {

			// If any of the data fits within the buffer...
			int spaceInBuffer = 0;
			int copyLength = 0;
			if (filePosition >= bufferStart) {
				spaceInBuffer = (int) ((bufferStart + buffer.length) - filePosition);
			}
			if (spaceInBuffer > 0) {

				// Copy as much as possible to the buffer.
				copyLength = (spaceInBuffer > len) ? len : spaceInBuffer;
				System.arraycopy(b, off, buffer,
						(int) (filePosition - bufferStart), copyLength);
				bufferModified = true;
				final long myDataEnd = filePosition + copyLength;
				dataEnd = (myDataEnd > dataEnd) ? myDataEnd : dataEnd;
				dataSize = (int) (dataEnd - bufferStart);
				filePosition += copyLength;
				// /System.out.println("--copy to buffer "+copyLength+" "+len);
			}

			// If there is any data remaining, move to the new position and copy
			// to
			// the new buffer.
			if (copyLength < len) {
				// System.out.println("--need more "+copyLength+" "+len+" space=
				// "+spaceInBuffer);
				seek(filePosition); // triggers a flush
				System.arraycopy(b, off + copyLength, buffer,
						(int) (filePosition - bufferStart), len - copyLength);
				bufferModified = true;
				final long myDataEnd = filePosition + (len - copyLength);
				dataEnd = (myDataEnd > dataEnd) ? myDataEnd : dataEnd;
				dataSize = (int) (dataEnd - bufferStart);
				filePosition += (len - copyLength);
			}

			// ...or write a lot of data...
		} else {

			// Flush the current buffer, and write this data to the eraf.
			if (bufferModified) {
				flush();
				bufferStart = dataEnd = dataSize = 0;
				// eraf.seek(filePosition); // JC added Oct 21, 2004
			}
			eraf.seek(filePosition); // moved per Steve Cerruti; Jan 14, 2005
			eraf.write(b, off, len);
			// System.out.println("--write at "+filePosition+" "+len);
			filePosition += len;
		}
	}

	/**
	 * Writes <code>b.length</code> bytes from the specified byte array
	 * starting at offset <code>off</code> to this eraf.
	 * 
	 * @param b
	 *            the data.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void write(byte b[]) throws IOException {
		writeBytes(b, 0, b.length);
	}

	/**
	 * Writes <code>len</code> bytes from the specified byte array starting at
	 * offset <code>off</code> to this eraf.
	 * 
	 * @param b
	 *            the data.
	 * @param off
	 *            the start offset in the data.
	 * @param len
	 *            the number of bytes to write.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void write(byte b[], int off, int len) throws IOException {
		writeBytes(b, off, len);
	}

	//
	// DataInput methods.
	//

	/**
	 * Reads a <code>boolean</code> from this eraf. This method reads a single
	 * byte from the eraf. A value of <code>0</code> represents
	 * <code>false</code>. Any other value represents <code>true</code>.
	 * This method blocks until the byte is read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the <code>boolean</code> value read.
	 * @exception EOFException
	 *                if this eraf has reached the end.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public boolean readBoolean() throws IOException {
		final int ch = this.read();
		if (ch < 0) {
			throw new EOFException();
		}
		return (ch != 0);
	}

	/**
	 * Reads a signed 8-bit value from this eraf. This method reads a byte from
	 * the eraf. If the byte read is <code>b</code>, where
	 * <code>0&nbsp;&lt;=&nbsp;b&nbsp;&lt;=&nbsp;255</code>, then the result
	 * is:
	 * <ul>
	 * <code>
	 *     (byte)(b)
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the byte is read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next byte of this eraf as a signed 8-bit <code>byte</code>.
	 * @exception EOFException
	 *                if this eraf has reached the end.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public byte readByte() throws IOException {
		final int ch = this.read();
		if (ch < 0) {
			throw new EOFException();
		}
		return (byte) (ch);
	}

	/**
	 * Reads an unsigned 8-bit number from this eraf. This method reads a byte
	 * from this eraf and returns that byte.
	 * <p>
	 * This method blocks until the byte is read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next byte of this eraf, interpreted as an unsigned 8-bit
	 *         number.
	 * @exception EOFException
	 *                if this eraf has reached the end.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public int readUnsignedByte() throws IOException {
		final int ch = this.read();
		if (ch < 0) {
			throw new EOFException();
		}
		return ch;
	}

	/**
	 * Reads a signed 16-bit number from this eraf. The method reads 2 bytes
	 * from this eraf. If the two bytes read, in order, are <code>b1</code>
	 * and <code>b2</code>, where each of the two values is between
	 * <code>0</code> and <code>255</code>, inclusive, then the result is
	 * equal to:
	 * <ul>
	 * <code>
	 *     (short)((b1 &lt;&lt; 8) | b2)
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the two bytes are read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next two bytes of this eraf, interpreted as a signed 16-bit
	 *         number.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading two bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public short readShort() throws IOException {
		final byte b[] = new byte[2];
		if (read(b, 0, 2) < 0) {
			throw new EOFException();
		}
		if (bigEndian) {
			return (short) (((b[0] & 0xFF) << 8) + (b[1] & 0xFF));
		} else {
			return (short) (((b[1] & 0xFF) << 8) + (b[0] & 0xFF));
		}
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void readShort(short[] pa, int start, int n) throws IOException {
		for (int i = start, end = n + start; i < end; i++) {
			pa[i] = readShort();
		}
	}

	/**
	 * Reads an unsigned 16-bit number from this eraf. This method reads two
	 * bytes from the eraf. If the bytes read, in order, are <code>b1</code>
	 * and <code>b2</code>, where
	 * <code>0&nbsp;&lt;=&nbsp;b1, b2&nbsp;&lt;=&nbsp;255</code>, then the
	 * result is equal to:
	 * <ul>
	 * <code>
	 *     (b1 &lt;&lt; 8) | b2
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the two bytes are read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next two bytes of this eraf, interpreted as an unsigned
	 *         16-bit integer.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading two bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public int readUnsignedShort() throws IOException {
		final byte b[] = new byte[2];
		if (read(b, 0, 2) < 0) {
			throw new EOFException();
		}
		if (bigEndian) {
			return ((b[0] & 0xFF) << 8) + (b[1] & 0xFF) & 0xFFFF;
		} else {
			return ((b[1] & 0xFF) << 8) + (b[0] & 0xFF) & 0xFFFF;
		}
	}

	/**
	 * Reads a Unicode character from this eraf. This method reads two bytes
	 * from the eraf. If the bytes read, in order, are <code>b1</code> and
	 * <code>b2</code>, where
	 * <code>0&nbsp;&lt;=&nbsp;b1,&nbsp;b2&nbsp;&lt;=&nbsp;255</code>, then
	 * the result is equal to:
	 * <ul>
	 * <code>
	 *     (char)((b1 &lt;&lt; 8) | b2)
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the two bytes are read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next two bytes of this eraf as a Unicode character.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading two bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public char readChar() throws IOException {
		final byte b[] = new byte[2];
		if (read(b, 0, 2) < 0) {
			throw new EOFException();
		}
		if (bigEndian) {
			return (char) (((b[0] & 0xFF) << 8) + (b[1] & 0xFF));
		} else {
			return (char) (((b[1] & 0xFF) << 8) + (b[0] & 0xFF));
		}
	}

	/**
	 * Reads a signed 32-bit integer from this eraf. This method reads 4 bytes
	 * from the eraf. If the bytes read, in order, are <code>b1</code>,
	 * <code>b2</code>, <code>b3</code>, and <code>b4</code>, where
	 * <code>0&nbsp;&lt;=&nbsp;b1, b2, b3, b4&nbsp;&lt;=&nbsp;255</code>,
	 * then the result is equal to:
	 * <ul>
	 * <code>
	 *     (b1 &lt;&lt; 24) | (b2 &lt;&lt; 16) + (b3 &lt;&lt; 8) + b4
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the four bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next four bytes of this eraf, interpreted as an
	 *         <code>int</code>.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading four bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public int readInt() throws IOException {
		final byte b[] = new byte[4];
		if (read(b, 0, 4) < 0) {
			throw new EOFException();
		}
		if (bigEndian) {
			return (((b[0] & 0xFF) << 24) + ((b[1] & 0xFF) << 16)
					+ ((b[2] & 0xFF) << 8) + ((b[3] & 0xFF)));
		} else {
			return (((b[3] & 0xFF) << 24) + ((b[2] & 0xFF) << 16)
					+ ((b[1] & 0xFF) << 8) + ((b[0] & 0xFF)));
		}
	}

	public long readUnsignedInt() throws IOException {
		// retaining only the first 4 bytes, ignoring sign when extending
		return ((long) readInt()) & 0xFFFFFFFFL;
	}

	/**
	 * Read an integer at the given position, bypassing all buffering.
	 * 
	 * @param pos
	 *            read a byte at this position
	 * @return The int that was read
	 * @throws IOException
	 */
	public int readIntUnbuffered(long pos) throws IOException {
		final byte[] bb = new byte[4];
		read_(pos, bb, 0, 4);
		final int ch1 = bb[0] & 0xFF;
		final int ch2 = bb[1] & 0xFF;
		final int ch3 = bb[2] & 0xFF;
		final int ch4 = bb[3] & 0xFF;
		if ((ch1 | ch2 | ch3 | ch4) < 0) {
			throw new EOFException();
		}

		if (bigEndian) {
			return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
		} else {
			return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
		}
	}

	/**
	 * Reads a signed 24-bit integer from this eraf. This method reads 3 bytes
	 * from the eraf. If the bytes read, in order, are <code>b1</code>,
	 * <code>b2</code>, and <code>b3</code>, where
	 * <code>0&nbsp;&lt;=&nbsp;b1, b2, b3&nbsp;&lt;=&nbsp;255</code>, then
	 * the result is equal to:
	 * <ul>
	 * <code>
	 *     (b1 &lt;&lt; 16) | (b2 &lt;&lt; 8) + (b3 &lt;&lt; 0)
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until the three bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * @exception EOFException
	 *                if this eraf reaches the end before reading four bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void readInt(int[] pa, int start, int n) throws IOException {
		for (int i = start, end = n + start; i < end; i++) {
			pa[i] = readInt();
		}
	}

	/**
	 * Reads a signed 64-bit integer from this eraf. This method reads eight
	 * bytes from the eraf. If the bytes read, in order, are <code>b1</code>,
	 * <code>b2</code>, <code>b3</code>, <code>b4</code>,
	 * <code>b5</code>, <code>b6</code>, <code>b7</code>, and
	 * <code>b8,</code> where:
	 * <ul>
	 * <code>
	 *     0 &lt;= b1, b2, b3, b4, b5, b6, b7, b8 &lt;=255,
	 * </code>
	 * </ul>
	 * <p>
	 * then the result is equal to:
	 * <p>
	 * <blockquote>
	 * 
	 * <pre>
	 * ((long) b1 &lt;&lt; 56) + ((long) b2 &lt;&lt; 48) + ((long) b3 &lt;&lt; 40) + ((long) b4 &lt;&lt; 32)
	 * 		+ ((long) b5 &lt;&lt; 24) + ((long) b6 &lt;&lt; 16) + ((long) b7 &lt;&lt; 8) + b8
	 * </pre>
	 * 
	 * </blockquote>
	 * <p>
	 * This method blocks until the eight bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next eight bytes of this eraf, interpreted as a
	 *         <code>long</code>.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading eight bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public long readLong() throws IOException {
		final byte b[] = new byte[8];
		if (read(b, 0, 8) < 0) {
			throw new EOFException();
		}
		if (bigEndian) {
			return (((b[0] & 0xFFL) << 56) + ((b[1] & 0xFFL) << 48)
					+ ((b[2] & 0xFFL) << 40) + ((b[3] & 0xFFL) << 32)
					+ ((b[4] & 0xFFL) << 24) + ((b[5] & 0xFFL) << 16)
					+ ((b[6] & 0xFFL) << 8) + ((b[7] & 0xFFL)));
		} else {
			return (((b[7] & 0xFFL) << 56) + ((b[6] & 0xFFL) << 48)
					+ ((b[5] & 0xFFL) << 40) + ((b[4] & 0xffL) << 32)
					+ ((b[3] & 0xFFL) << 24) + ((b[2] & 0xFFL) << 16)
					+ ((b[1] & 0xFFL) << 8) + ((b[0] & 0xFFL)));
		}
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void readLong(long[] pa, int start, int n) throws IOException {
		for (int i = start, end = n + start; i < end; i++) {
			pa[i] = readLong();
		}
	}

	/**
	 * Reads a <code>float</code> from this eraf. This method reads an
	 * <code>int</code> value as if by the <code>readInt</code> method and
	 * then converts that <code>int</code> to a <code>float</code> using the
	 * <code>intBitsToFloat</code> method in class <code>Float</code>.
	 * <p>
	 * This method blocks until the four bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next four bytes of this eraf, interpreted as a
	 *         <code>float</code>.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading four bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.io.RandomAccessFile#readInt()
	 * @see java.lang.Float#intBitsToFloat(int)
	 */
	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void readFloat(float[] pa, int start, int n) throws IOException {
		for (int i = start, end = n + start; i < end; i++) {
			pa[i] = Float.intBitsToFloat(readInt());
		}
	}

	/**
	 * Reads a <code>double</code> from this eraf. This method reads a
	 * <code>long</code> value as if by the <code>readLong</code> method and
	 * then converts that <code>long</code> to a <code>double</code> using
	 * the <code>longBitsToDouble</code> method in class <code>Double</code>.
	 * <p>
	 * This method blocks until the eight bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next eight bytes of this eraf, interpreted as a
	 *         <code>double</code>.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading eight bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.io.RandomAccessFile#readLong()
	 * @see java.lang.Double#longBitsToDouble(long)
	 */
	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void readDouble(double[] pa, int start, int n) throws IOException {
		for (int i = start, end = n + start; i < end; i++) {
			pa[i] = Double.longBitsToDouble(readLong());
		}
	}

	/**
	 * Reads the next line of text from this eraf. This method successively
	 * reads bytes from the eraf until it reaches the end of a line of text.
	 * <p>
	 * 
	 * A line of text is terminated by a carriage-return character (<code>'&#92;r'</code>),
	 * a newline character (<code>'&#92;n'</code>), a carriage-return
	 * character immediately followed by a newline character, or the end of the
	 * input stream. The line-terminating character(s), if any, are included as
	 * part of the string returned.
	 * 
	 * <p>
	 * This method blocks until a newline character is read, a carriage return
	 * and the byte following it are read (to see if it is a newline), the end
	 * of the stream is detected, or an exception is thrown.
	 * 
	 * @return the next line of text from this eraf.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @task we can optimize this
	 */
	public String readLine() throws IOException {
		final StringBuilder input = new StringBuilder();
		int c;

		while (((c = read()) != -1) && (c != '\n')) {
			input.append((char) c);
		}
		if ((c == -1) && (input.length() == 0)) {
			return null;
		}
		return input.toString();
	}

	/**
	 * Reads in a string from this eraf. The string has been encoded using a
	 * modified UTF-8 format.
	 * <p>
	 * The first two bytes are read as if by <code>readUnsignedShort</code>.
	 * This value gives the number of following bytes that are in the encoded
	 * string, not the length of the resulting string. The following bytes are
	 * then interpreted as bytes encoding characters in the UTF-8 format and are
	 * converted into characters.
	 * <p>
	 * This method blocks until all the bytes are read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return a Unicode string.
	 * @exception EOFException
	 *                if this eraf reaches the end before reading all the bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @exception UTFDataFormatException
	 *                if the bytes do not represent valid UTF-8 encoding of a
	 *                Unicode string.
	 * @see java.io.RandomAccessFile#readUnsignedShort()
	 */
	public String readUTF() throws IOException {
		return DataInputStream.readUTF(this);
	}

	/**
	 * Read a String of knoen length.
	 * 
	 * @param nbytes
	 *            number of bytes to read
	 * @return String wrapping the bytes.
	 * @throws IOException
	 */
	public String readString(int nbytes) throws IOException {
		final byte[] data = new byte[nbytes];
		readFully(data);
		return new String(data);
	}

	//
	// DataOutput methods.
	//

	/**
	 * Writes a <code>boolean</code> to the eraf as a 1-byte value. The value
	 * <code>true</code> is written out as the value <code>(byte)1</code>;
	 * the value <code>false</code> is written out as the value
	 * <code>(byte)0</code>.
	 * 
	 * @param v
	 *            a <code>boolean</code> value to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void writeBoolean(boolean v) throws IOException {
		write(v ? 1 : 0);
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void writeBoolean(boolean[] pa, int start, int n) throws IOException {
		for (int i = start, end = start + n; i < end; i++) {
			writeBoolean(pa[i]);
		}
	}

	/**
	 * Writes a <code>byte</code> to the eraf as a 1-byte value.
	 * 
	 * @param v
	 *            a <code>byte</code> value to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void writeByte(int v) throws IOException {
		write(v);
	}

	/**
	 * Writes a <code>short</code> to the eraf as two bytes, high byte first.
	 * 
	 * @param v
	 *            a <code>short</code> to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void writeShort(int v) throws IOException {
		write((v >>> 8) & 0xFF);
		write((v >>> 0) & 0xFF);
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void writeShort(short[] pa, int start, int n) throws IOException {
		for (int i = start, end = start + n; i < end; i++) {
			writeShort(pa[i]);
		}
	}

	/**
	 * Writes a <code>char</code> to the eraf as a 2-byte value, high byte
	 * first.
	 * 
	 * @param v
	 *            a <code>char</code> value to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void writeChar(int v) throws IOException {
		write((v >>> 8) & 0xFF);
		write((v >>> 0) & 0xFF);
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void writeChar(char[] pa, int start, int n) throws IOException {
		for (int i = start, end = start + n; i < end; i++) {
			writeChar(pa[i]);
		}
	}

	/**
	 * Writes an <code>int</code> to the eraf as four bytes, high byte first.
	 * 
	 * @param v
	 *            an <code>int</code> to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void writeInt(int v) throws IOException {
		write((v >>> 24) & 0xFF);
		write((v >>> 16) & 0xFF);
		write((v >>> 8) & 0xFF);
		write((v >>> 0) & 0xFF);
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void writeInt(int[] pa, int start, int n) throws IOException {
		for (int i = start, end = start + n; i < end; i++) {
			writeInt(pa[i]);
		}
	}

	/**
	 * Writes a <code>long</code> to the eraf as eight bytes, high byte first.
	 * 
	 * @param v
	 *            a <code>long</code> to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void writeLong(long v) throws IOException {

		if (bigEndian) {
			write((int) (v >>> 56) & 0xFF);
			write((int) (v >>> 48) & 0xFF);
			write((int) (v >>> 40) & 0xFF);
			write((int) (v >>> 32) & 0xFF);
			write((int) (v >>> 24) & 0xFF);
			write((int) (v >>> 16) & 0xFF);
			write((int) (v >>> 8) & 0xFF);
			write((int) (v >>> 0) & 0xFF);
		} else {
			write((int) (v >>> 0) & 0xFF);
			write((int) (v >>> 8) & 0xFF);
			write((int) (v >>> 16) & 0xFF);
			write((int) (v >>> 24) & 0xFF);	
			write((int) (v >>> 32) & 0xFF);	
			write((int) (v >>> 40) & 0xFF);
			write((int) (v >>> 48) & 0xFF);	
			write((int) (v >>> 56) & 0xFF);
			
		}		
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void writeLong(long[] pa, int start, int n) throws IOException {
		for (int i = start, end = start + n; i < end; i++) {
			writeLong(pa[i]);
		}
	}

	/**
	 * Converts the float argument to an <code>int</code> using the
	 * <code>floatToIntBits</code> method in class <code>Float</code>, and
	 * then writes that <code>int</code> value to the eraf as a 4-byte
	 * quantity, high byte first.
	 * 
	 * @param v
	 *            a <code>float</code> value to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.lang.Float#floatToIntBits(float)
	 */
	public void writeFloat(float v) throws IOException {
		writeInt(Float.floatToIntBits(v));
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void writeFloat(float[] pa, int start, int n) throws IOException {
		for (int i = start, end = start + n; i < end; i++) {
			writeFloat(pa[i]);
		}
	}

	/**
	 * Converts the double argument to a <code>long</code> using the
	 * <code>doubleToLongBits</code> method in class <code>Double</code>,
	 * and then writes that <code>long</code> value to the eraf as an 8-byte
	 * quantity, high byte first.
	 * 
	 * @param v
	 *            a <code>double</code> value to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.lang.Double#doubleToLongBits(double)
	 */
	public void writeDouble(double v) throws IOException {
		writeLong(Double.doubleToLongBits(v));
	}

	/**
	 * _more_
	 * 
	 * @param pa
	 *            _more_
	 * @param start
	 *            _more_
	 * @param n
	 *            _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void writeDouble(double[] pa, int start, int n) throws IOException {
		for (int i = start, end = start + n; i < end; i++) {
			writeDouble(pa[i]);
		}
	}

	/**
	 * Writes the string to the eraf as a sequence of bytes. Each character in
	 * the string is written out, in sequence, by discarding its high eight
	 * bits.
	 * 
	 * @param s
	 *            a string of bytes to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void writeBytes(String s) throws IOException {
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			write((byte) s.charAt(i));
		}
	}

	/**
	 * Writes the character array to the eraf as a sequence of bytes. Each
	 * character in the string is written out, in sequence, by discarding its
	 * high eight bits.
	 * 
	 * @param b
	 *            a character array of bytes to be written.
	 * @param off
	 *            the index of the first character to write.
	 * @param len
	 *            the number of characters to write.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void writeBytes(char b[], int off, int len) throws IOException {
		for (int i = off; i < len; i++) {
			write((byte) b[i]);
		}
	}

	/**
	 * Writes a string to the eraf as a sequence of characters. Each character
	 * is written to the data output stream as if by the <code>writeChar</code>
	 * method.
	 * 
	 * @param s
	 *            a <code>String</code> value to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.io.RandomAccessFile#writeChar(int)
	 */
	public void writeChars(String s) throws IOException {
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			final int v = s.charAt(i);
			write((v >>> 8) & 0xFF);
			write((v >>> 0) & 0xFF);
		}
	}

	/**
	 * Writes a string to the eraf using UTF-8 encoding in a machine-independent
	 * manner.
	 * <p>
	 * First, two bytes are written to the eraf as if by the
	 * <code>writeShort</code> method giving the number of bytes to follow.
	 * This value is the number of bytes actually written out, not the length of
	 * the string. Following the length, each character of the string is output,
	 * in sequence, using the UTF-8 encoding for each character.
	 * 
	 * @param str
	 *            a string to be written.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public void writeUTF(String str) throws IOException {
		final int strlen = str.length();
		int utflen = 0;

		for (int i = 0; i < strlen; i++) {
			final int c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}
		if (utflen > 65535) {
			throw new UTFDataFormatException();
		}

		write((utflen >>> 8) & 0xFF);
		write((utflen >>> 0) & 0xFF);
		for (int i = 0; i < strlen; i++) {
			final int c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				write(c);
			} else if (c > 0x07FF) {
				write(0xE0 | ((c >> 12) & 0x0F));
				write(0x80 | ((c >> 6) & 0x3F));
				write(0x80 | ((c >> 0) & 0x3F));
			} else {
				write(0xC0 | ((c >> 6) & 0x1F));
				write(0x80 | ((c >> 0) & 0x3F));
			}
		}
	}

	/**
	 * Create a string representation of this object.
	 * 
	 * @return a string representation of the state of the object.
	 */
	public String toString() {
		return "fp=" + filePosition + ", bs=" + bufferStart + ", de=" + dataEnd
				+ ", ds=" + dataSize + ", bl=" + buffer.length + ", readonly="
				+ readonly + ", bm=" + bufferModified;
	}

	/** Support for FileCache. */
	protected boolean cached;

	protected String location;

	/**
	 * _more_
	 * 
	 * @param cached
	 *            _more_
	 */
	public void setCached(boolean cached) {
		this.cached = cached;
	}

	/**
	 * _more_
	 * 
	 * @return _more_
	 */
	public boolean isCached() {
		return cached;
	}

	/**
	 * _more_
	 * 
	 * @throws IOException
	 *             _more_
	 */
	public void synch() throws IOException {
	}

	public String getLocation() {
		return location;
	}

}
