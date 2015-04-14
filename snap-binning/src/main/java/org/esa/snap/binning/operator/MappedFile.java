package org.esa.snap.binning.operator;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Norman
 */
public class MappedFile {

    private final File file;
    private final FileChannel.MapMode mapMode;
    private RandomAccessFile raf;
    private MappedByteBuffer buffer;

    private MappedFile(File file, FileChannel.MapMode mapMode) throws IOException {
        this.file = file;
        this.mapMode = mapMode;
    }

    public static MappedFile open(File file, FileChannel.MapMode mapMode, long position, long size) throws IOException {
        MappedFile mappedFile = new MappedFile(file, mapMode);
        mappedFile.map(position, size);
        return mappedFile;
    }

    public static MappedFile open(File file, long size) throws IOException {
        return open(file, FileChannel.MapMode.READ_WRITE, 0, size);
    }

    public static MappedFile open(File file) throws IOException {
        return open(file, FileChannel.MapMode.READ_ONLY, 0, file.length());
    }

    public File getFile() {
        return file;
    }

    public MappedByteBuffer getBuffer() {
        return buffer;
    }

    public MappedByteBuffer remap(long position, long size) throws IOException {
        close();
        map(position, size);
        return getBuffer();
    }

    public void close() throws IOException {
        this.raf.close();
    }

    private void map(long position, long size) throws IOException {
        raf = new RandomAccessFile(file, mapMode == FileChannel.MapMode.READ_WRITE ? "rw" : "r");
        buffer = raf.getChannel().map(mapMode, position, size);
    }
}
