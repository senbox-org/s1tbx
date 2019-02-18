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

/**
 * Byte Channel for Object Storage VFS.
 * A byte channel that maintains a current <i>position</i> and allows the
 * position to be changed.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
class ObjectStorageByteChannel implements SeekableByteChannel {

    private static final int CONNECT_MODE_READ = 1;
    private static final int CONNECT_MODE_UPLOAD = 2;
    private static final int CONNECT_MODE_WRITE = 3;
    private static final int CONNECT_MODE_DELETE = 4;

    private final ObjectStoragePath path;
    private final URL url;
    private URLConnection connection;
    private long position;
    private long contentLength;

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
     * @return <tt>true</tt> if, and only if, this channel is open
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
            throw new IOException(ex);
        }
    }

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * @param dst The buffer
     * @return the number of bytes actually read
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
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * @param dsts   The buffers array
     * @param offset The offset
     * @param length The length
     * @return the number of bytes actually read
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
     * @return the number of bytes actually written
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
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * @param srcs   The source ByteBuffer array
     * @param offset The offset
     * @param length The length
     * @return the number of bytes actually written
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
     * Truncates the entity, to which this channel is connected, to the given
     * size.
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

    private void assertOpen() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

//    private void skipBytes(InputStream stream) throws IOException {
//        if (stream.skip(position) != position) {
//            throw new IOException("\nUnable to skip " + position + " bytes from " + stream.available() + " remaining bytes.");
//        }
//    }

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

    private void writeBytes(OutputStream stream, byte[] array, int offset, int length) throws IOException {
        stream.write(array, offset, length);
    }

    private void connect(int connect_mode) throws IOException {
        String mode = "GET";
        switch (connect_mode) {
            case CONNECT_MODE_READ: {
                mode = "GET";
                break;
            }
            case CONNECT_MODE_UPLOAD: {
                mode = "POST";
                break;
            }
            case CONNECT_MODE_WRITE: {
                mode = "PUT";
                break;
            }
            case CONNECT_MODE_DELETE: {
                mode = "DELETE";
                break;
            }
        }
        Map<String, String> requestProperties = new HashMap<>();
        if (position > 0 && contentLength > 0) {
            String rangeSpec = "bytes=" + position + "-" + (contentLength - 1);
            requestProperties.put("Range", rangeSpec);
        }
        connection = ((ObjectStorageFileSystemProvider) path.getFileSystem().provider()).getProviderConnectionChannel(url, mode, requestProperties);
    }
}
