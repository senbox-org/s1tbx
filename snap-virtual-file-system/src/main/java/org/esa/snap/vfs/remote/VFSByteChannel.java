package org.esa.snap.vfs.remote;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

/**
 * Byte Channel for VFS.
 * A byte channel that maintains a current <i>position</i> and allows the
 * position to be changed.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
class VFSByteChannel implements SeekableByteChannel {

    private final VFSPath path;
    private final long contentLength;

    private HttpURLConnection connection;
    private long position;
    private boolean samePosition;

    /**
     * Creates the new byte channel for  VFS
     *
     * @param path The VFS path for which new byte channel is created
     * @throws IOException If an I/O error occurs
     */
    VFSByteChannel(VFSPath path) throws IOException {
        this.path = path;
        this.position = 0;

        this.connection = VFSFileChannel.buildProviderConnectionChannel(this.path, this.position, "GET");
        this.contentLength = connection.getContentLengthLong();
        this.samePosition = false;
    }

    /**
     * Returns the current size of entity to which this channel is connected.
     *
     * @return The current size, measured in bytes
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    @Override
    public long size() throws IOException {
        assertOpen();
        return this.contentLength;
    }

    /**
     * Returns this channel's position.
     *
     * @return This channel's position, a non-negative integer counting the number of bytes
     * from the beginning of the entity to the current position
     * @throws ClosedChannelException If this channel is closed
     */
    @Override
    public long position() throws ClosedChannelException {
        assertOpen();
        return this.position;
    }

    /**
     * Sets this channel's position.
     *
     * @param newPosition The new position, a non-negative integer counting
     *                    the number of bytes from the beginning of the entity
     * @return This channel
     * @throws ClosedChannelException   If this channel is closed
     * @throws IllegalArgumentException If the new position is negative
     * @throws IOException              If some other I/O error occurs
     */
    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        assertOpen();
        if (newPosition < 0) {
            throw new IllegalArgumentException("newPosition is negative");
        }
        if (newPosition >= this.contentLength) {
            throw new EOFException(this.path.toString());
        }
        this.samePosition = (this.position == newPosition);
        this.position = newPosition;
        return this;
    }

    /**
     * Tells whether or not this channel is open.
     *
     * @return {@code true} if this channel is open
     */
    @Override
    public boolean isOpen() {
        return (this.connection != null);
    }

    /**
     * Closes this channel.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (this.connection != null) {
            this.connection.disconnect();
            this.connection = null;
        }
        this.path.getFileSystem().removeByteChannel(this);
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * @param destinationBuffer The buffer
     * @return The number of bytes actually read
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If an I/O error occurs
     */
    @Override
    public int read(ByteBuffer destinationBuffer) throws IOException {
        assertOpen();
        if (this.position >= this.contentLength) {
            return -1;
        }

        if (!this.samePosition) {
            this.connection.disconnect();
            this.connection = VFSFileChannel.buildProviderConnectionChannel(this.path, this.position, "GET");
        }
        InputStream inputStream = this.connection.getInputStream();

        int offset;
        byte[] bytes;
        int bytesToRead = destinationBuffer.remaining();
        if (destinationBuffer.hasArray()) {
            bytes = destinationBuffer.array();
            offset = destinationBuffer.arrayOffset();
        } else {
            bytes = new byte[bytesToRead];
            offset = 0;
        }
        int bytesTransferred = readBytes(inputStream, bytes, offset, bytesToRead);
        destinationBuffer.put(bytes, offset, bytesTransferred);
        return bytesTransferred;
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffers.
     *
     * @param destinationBuffers   The buffers array
     * @param offset The offset
     * @param length The length
     * @return The number of bytes actually read
     */
    long read(ByteBuffer[] destinationBuffers, int offset, int length) throws IOException {
        assertOpen();
        if (this.position >= this.contentLength) {
            return -1;
        }

        if (!this.samePosition) {
            this.connection.disconnect();
            this.connection = VFSFileChannel.buildProviderConnectionChannel(this.path, this.position, "GET");
        }
        InputStream inputStream = this.connection.getInputStream();

        long bytesRead = 0;
        for (ByteBuffer buffer : destinationBuffers) {
            int numRemaining = buffer.remaining();
            if (buffer.hasArray()) {
                byte[] bytes = buffer.array();
                numRemaining = readBytes(inputStream, bytes, offset, length);
                buffer.put(bytes, buffer.arrayOffset(), numRemaining);
            } else {
                byte[] bytes = new byte[numRemaining];
                numRemaining = readBytes(inputStream, bytes, offset, length);
                buffer.put(bytes, 0, numRemaining);
            }
            bytesRead += numRemaining;
        }
        return bytesRead;
    }

    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * @param src The buffer
     * @return The number of bytes actually written
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    @Override
    public int write(ByteBuffer src) throws IOException {
        assertOpen();
        this.connection = VFSFileChannel.buildProviderConnectionChannel(this.path, this.position, "PUT");
        OutputStream stream = this.connection.getOutputStream();
        int numRemaining = src.remaining();
        int numWritten = 0;
        if (src.hasArray()) {
            byte[] bytes = src.array();
            writeBytes(stream, bytes, src.arrayOffset(), numRemaining);
            numWritten = bytes.length;
        } else {
            while (numRemaining > 0) {
                int length = numRemaining;
                byte[] bytes = new byte[length];
                src.get(bytes, numWritten, length);
                writeBytes(stream, bytes, 0, numRemaining);
                numWritten += length;
                numRemaining -= length;
            }
            throw new NonWritableChannelException();
        }
        return numWritten;
    }

    /**
     * Writes a sequence of bytes to this channel from the given buffers.
     *
     * @param srcs   The source ByteBuffer array
     * @param offset The offset
     * @param length The length
     * @return The number of bytes actually written
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        assertOpen();
        this.connection = VFSFileChannel.buildProviderConnectionChannel(this.path, this.position, "PUT");
        long bytesWritten = 0;
        OutputStream stream = this.connection.getOutputStream();
        for (ByteBuffer src : srcs) {
            int numRemaining = src.remaining();
            int numWritten = 0;
            if (src.hasArray()) {
                byte[] bytes = src.array();
                writeBytes(stream, bytes, offset, length);
                numWritten = bytes.length;
            } else {
                while (numRemaining > 0) {
                    int size = numRemaining;
                    byte[] bytes = new byte[size];
                    src.get(bytes, numWritten, size);
                    writeBytes(stream, bytes, offset, length);
                    numWritten += size;
                    numRemaining -= size;
                }
                throw new NonWritableChannelException();
            }
            bytesWritten += numWritten;
        }
        return bytesWritten;
    }

    /**
     * Truncates the entity, to which this channel is connected, to the given size.
     *
     * @param size The new size, a non-negative byte count
     * @return This channel
     * @throws NonWritableChannelException If this channel was not opened for writing
     * @throws IllegalArgumentException    If the new size is negative
     */
    @Override
    public SeekableByteChannel truncate(long size) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks channel is open.
     *
     * @throws ClosedChannelException If this channel is closed
     */
    private void assertOpen() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    /**
     * Reads up to <code>length</code> bytes of data from the given input stream into an array of bytes.
     * An attempt is made to read as many as <code>length</code> bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     *
     * @param inputStream The input stream from which the data is read
     * @param array       The buffer into which the data is read
     * @param offset      The start offset in array <code>array</code> at which the data is written
     * @param length      The maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no more data because the end of the stream has been reached
     * @throws IOException If some other I/O error occurs
     */
    private int readBytes(InputStream inputStream, byte[] array, int offset, int length) throws IOException {
        int bytesTransferred = 0;
        while (length > 0) {
            int bytesReadNow = inputStream.read(array, offset, length);
            if (bytesReadNow <= 0) {
                break;
            }
            length -= bytesReadNow;
            offset += bytesReadNow;
            bytesTransferred += bytesReadNow;
            this.position += bytesReadNow;
        }
        return bytesTransferred;
    }

    /**
     * Writes <code>length</code> bytes from the specified byte array starting at offset <code>offset</code> to the given output stream.
     *
     * @param stream The output stream to which the data is written
     * @param array  The buffer from which the data is read
     * @param offset The start offset in array <code>array</code> from which the data is read
     * @param length The number of bytes to write
     * @throws IOException If some other I/O error occurs
     */
    private void writeBytes(OutputStream stream, byte[] array, int offset, int length) throws IOException {
        stream.write(array, offset, length);
    }
}
