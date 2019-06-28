package org.esa.snap.dataio;

import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;

class FileChannelImageInputStream extends ImageInputStreamImpl {

    private FileChannelInputStream fileChannelInputStream;

    FileChannelImageInputStream(FileChannelInputStream fileChannelInputStream) throws IOException {
        super();

        this.fileChannelInputStream = fileChannelInputStream;

        this.streamPos = fileChannelInputStream.position();
        this.flushedPos = this.streamPos;
        this.bitOffset = 0;
    }

    @Override
    public int read() throws IOException {
        checkClosed();

        this.bitOffset = 0;
        int byteValue = this.fileChannelInputStream.read();
        if (byteValue != -1) {
            ++this.streamPos;
        }
        return byteValue;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();

        this.bitOffset = 0;
        int bytesRead = this.fileChannelInputStream.read(b, off, len);
        if (bytesRead != -1) {
            this.streamPos += bytesRead;
        }
        return bytesRead;
    }

    @Override
    public long length() {
        try {
            checkClosed();

            return this.fileChannelInputStream.length();
        } catch (IOException e) {
            return -1L;
        }
    }

    @Override
    public void seek(long newPosition) throws IOException {
        checkClosed();

        if (newPosition < this.flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        this.bitOffset = 0;
        this.fileChannelInputStream.seek(newPosition);
        this.streamPos = newPosition;
    }

    @Override
    public void close() throws IOException {
        super.close();

        this.fileChannelInputStream.close();
        this.fileChannelInputStream = null;
    }
}
