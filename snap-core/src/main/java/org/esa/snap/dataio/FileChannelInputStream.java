package org.esa.snap.dataio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by jcoravu on 16/5/2019.
 */
class FileChannelInputStream extends InputStream {

    private FileChannel fileChannel;

    private ByteBuffer byteBuffer;
    private byte[] buffer;
    private byte[] oneByteBuffer;

    FileChannelInputStream(FileChannel fileChannel) {
        if (fileChannel == null) {
            throw new NullPointerException("The file channel is null.");
        }
        this.fileChannel = fileChannel;
    }

    @Override
    public int read() throws IOException {
        checkClosed();

        if (this.oneByteBuffer == null) {
            this.oneByteBuffer = new byte[1];
        }

        int bytesRead = read(this.oneByteBuffer);
        return (bytesRead == 1) ? (this.oneByteBuffer[0] & 255) : -1;
    }

    @Override
    public int read(byte[] outputBuffer, int offset, int length) throws IOException {
        checkClosed();

        if (offset >= 0 && offset <= outputBuffer.length && length >= 0 && offset + length <= outputBuffer.length && offset + length >= 0) {
            if (length == 0) {
                return 0;
            } else {
                if (this.buffer == null || this.buffer != outputBuffer) {
                    this.byteBuffer = ByteBuffer.wrap(outputBuffer);
                }
                this.buffer = outputBuffer;

                this.byteBuffer.limit(Math.min(offset + length, this.byteBuffer.capacity()));
                this.byteBuffer.position(offset);

                return this.fileChannel.read(this.byteBuffer);
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public void close() throws IOException {
        this.fileChannel.close();
        this.fileChannel = null;

        this.byteBuffer = null;
        this.buffer = null;
        this.oneByteBuffer = null;
    }

    public long length() throws IOException {
        checkClosed();

        return this.fileChannel.size();
    }

    public void seek(long newPosition) throws IOException {
        checkClosed();

        this.fileChannel.position(newPosition);
    }

    public long position() throws IOException {
        return this.fileChannel.position();
    }

    private void checkClosed() throws IOException {
        if (this.fileChannel == null) {
            throw new IOException("The file channel is already closed.");
        }
    }
}
