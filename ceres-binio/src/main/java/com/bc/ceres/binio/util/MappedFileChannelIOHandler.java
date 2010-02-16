package com.bc.ceres.binio.util;

import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.IOHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedFileChannelIOHandler implements IOHandler {

    private final FileChannel channel;

    private MappedByteBuffer mappedBuffer;
    private long mappedPos;
    private long mappedUpperBound;
    private long streamPos;

    public MappedFileChannelIOHandler(FileChannel channel) throws IOException {
        if (channel == null) {
            throw new IllegalArgumentException("channel == null");
        }
        if (!channel.isOpen()) {
            throw new IllegalArgumentException("channel.isOpen() == false");
        }

        this.channel = channel;
        long channelPosition = channel.position();
        streamPos = channelPosition;
        long fullSize = channel.size() - channelPosition;
        long mappedSize = Math.min(fullSize, 2147483647L);
        mappedPos = 0L;
        mappedUpperBound = mappedPos + mappedSize;
        mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, channelPosition, mappedSize);
    }

    @Override
    public void read(DataContext context, byte[] data, long position) throws IOException {
        synchronized (this) {
            seek(position);
            read(data);
        }
    }

    @Override
    public void write(DataContext context, byte[] data, long position) throws IOException {
        throw new RuntimeException("not implemented");
    }
    
    @Override
    public long getMaxPosition() throws IOException {
        synchronized (this) {
            return channel.size();
        }
    }

    private void seek(long pos) throws IOException {
        streamPos = pos;
        if (pos >= mappedPos && pos < mappedUpperBound) {
            mappedBuffer.position((int) (pos - mappedPos));
        } else {
            int len = (int) Math.min(channel.size() - pos, 2147483647L);
            mappedBuffer = getMappedBuffer(len);
        }
    }

    private int read(byte b[]) throws IOException {
        if (b.length == 0) {
            return 0;
        }
        final int len = b.length;
        ByteBuffer byteBuffer = getMappedBuffer(len);
        byteBuffer.get(b, 0, len);
        streamPos += len;
        return len;
    }

    private MappedByteBuffer getMappedBuffer(int len) throws IOException {
        if (streamPos < mappedPos || streamPos + (long) len >= mappedUpperBound) {
            mappedPos = streamPos;
            long mappedSize = Math.min(channel.size() - mappedPos, 2147483647L);
            mappedUpperBound = mappedPos + mappedSize;
            mappedBuffer = channel.map(
                    FileChannel.MapMode.READ_ONLY, mappedPos, mappedSize);
        }
        return mappedBuffer;
    }
}
