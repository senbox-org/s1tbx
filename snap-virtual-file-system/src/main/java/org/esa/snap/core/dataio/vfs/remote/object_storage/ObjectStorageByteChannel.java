package org.esa.snap.core.dataio.vfs.remote.object_storage;

import javax.net.ssl.HttpsURLConnection;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Byte Channel for Object Storage VFS.
 * A byte channel that maintains a current <i>position</i> and allows the
 * position to be changed.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
class ObjectStorageByteChannel implements SeekableByteChannel {

    /**
     * The connect type flag for this channel: GET
     */
    private static final int CONNECT_MODE_READ = 1;

    /**
     * The connect type flag for this channel: POST
     */
    private static final int CONNECT_MODE_UPLOAD = 2;

    /**
     * The connect type flag for this channel: PUT
     */
    private static final int CONNECT_MODE_WRITE = 3;

    /**
     * The connect type flag for this channel: DELETE
     */
    private static final int CONNECT_MODE_DELETE = 4;

    private static Logger logger = Logger.getLogger(ObjectStorageByteChannel.class.getName());

    private final ObjectStoragePath path;
    private final URL url;
    private URLConnection connection;
    private long position;
    private long contentLength;

    /**
     * Creates the new byte channel for Object Storage VFS
     *
     * @param path The VFS path for which new byte channel is created
     * @throws IOException If an I/O error occurs
     */
    ObjectStorageByteChannel(ObjectStoragePath path) throws IOException {
        String root = path.getRoot().toString();
        path = path.startsWith(root) ? ObjectStoragePath.parsePath((ObjectStorageFileSystem) path.getFileSystem(), path.toString().replaceAll(root, "/")) : path;
        this.path = path;
        this.url = path.getFileURL();
        this.position = 0;
        connect(CONNECT_MODE_READ);
        this.contentLength = connection.getContentLengthLong();
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
        return contentLength;
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
        return position;
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
        if (newPosition > contentLength) {
            throw new EOFException(url.toString());
        }
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
        return connection != null;
    }

    /**
     * Closes this channel.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        try {
            if (connection != null) {
                if (url.getProtocol().equals("https")) {
                    ((HttpsURLConnection) connection).disconnect();
                } else {
                    ((HttpURLConnection) connection).disconnect();
                }
                connection = null;
            }
            ((ObjectStorageFileSystem) path.getFileSystem()).removeByteChannel(this);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to close the Byte Channel. Details: " + ex.getMessage());
            throw new IOException(ex);
        }
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * @param dst The buffer
     * @return The number of bytes actually read
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If an I/O error occurs
     */
    @Override
    public int read(ByteBuffer dst) throws IOException {
        assertOpen();
        if (position >= contentLength) {
            throw new EOFException(url.toString());
        }
        connect(CONNECT_MODE_READ);
        InputStream stream = connection.getInputStream();
        int numRemaining = dst.remaining();
        if (dst.hasArray()) {
            byte[] bytes = dst.array();
            numRemaining = readBytes(stream, bytes, dst.arrayOffset(), numRemaining);
            dst.put(bytes, dst.arrayOffset(), numRemaining);
        } else {
            byte[] bytes = new byte[numRemaining];
            numRemaining = readBytes(stream, bytes, 0, numRemaining);
            dst.put(bytes, 0, numRemaining);
        }
        return numRemaining;
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffers.
     *
     * @param dsts   The buffers array
     * @param offset The offset
     * @param length The length
     * @return The number of bytes actually read
     */
    long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        assertOpen();
        if (position >= contentLength) {
            throw new EOFException(url.toString());
        }
        connect(CONNECT_MODE_READ);
        long bytesRead = 0;
        InputStream stream = connection.getInputStream();
        for (ByteBuffer dst : dsts) {
            int numRemaining = dst.remaining();
            if (dst.hasArray()) {
                byte[] bytes = dst.array();
                numRemaining = readBytes(stream, bytes, offset, length);
                dst.put(bytes, dst.arrayOffset(), numRemaining);
            } else {
                byte[] bytes = new byte[numRemaining];
                numRemaining = readBytes(stream, bytes, offset, length);
                dst.put(bytes, 0, numRemaining);
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
        connect(CONNECT_MODE_WRITE);
        OutputStream stream = connection.getOutputStream();
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
        connect(CONNECT_MODE_WRITE);
        long bytesWritten = 0;
        OutputStream stream = connection.getOutputStream();
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
     * @throws ClosedChannelException      If this channel is closed
     * @throws IllegalArgumentException    If the new size is negative
     * @throws IOException                 If some other I/O error occurs
     */
    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new IOException(new UnsupportedOperationException());
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
     * @param stream The input stream from which the data is read
     * @param array  The buffer into which the data is read
     * @param offset The start offset in array <code>array</code> at which the data is written
     * @param length The maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no more data because the end of the stream has been reached
     * @throws IOException If some other I/O error occurs
     */
    private int readBytes(InputStream stream, byte[] array, int offset, int length) throws IOException {
        int bytesRead = 0;
        while (length > 0) {
            int bytesReadNow = stream.read(array, offset, length);
            if (bytesReadNow < 0) {
                return bytesRead > 0 ? bytesRead : bytesReadNow;
            }
            length -= bytesReadNow;
            offset += bytesReadNow;
            bytesRead += bytesReadNow;
            position += bytesReadNow;
        }
        return bytesRead;
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

    /**
     * Establish the connection with the VFS service.
     *
     * @param connectMode The connect type flag
     * @throws IOException If some other I/O error occurs
     */
    private void connect(int connectMode) throws IOException {
        String mode = "GET";
        switch (connectMode) {
            case CONNECT_MODE_READ:
                mode = "GET";
                break;
            case CONNECT_MODE_UPLOAD:
                mode = "POST";
                break;
            case CONNECT_MODE_WRITE:
                mode = "PUT";
                break;
            case CONNECT_MODE_DELETE:
                mode = "DELETE";
                break;
            default:
                break;
        }
        Map<String, String> requestProperties = new HashMap<>();
        if (position > 0 && contentLength > 0) {
            String rangeSpec = "bytes=" + position + "-" + (contentLength - 1);
            requestProperties.put("Range", rangeSpec);
        }
        connection = ((ObjectStorageFileSystemProvider) path.getFileSystem().provider()).getProviderConnectionChannel(url, mode, requestProperties);
    }
}
