package org.esa.snap.vfs.remote;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;

/**
 * The utility class for doing remote IO operations.
 *
 * @author Jean Coravu
 */
public class TransferFileContentUtil {

    private TransferFileContentUtil() {
        //instantiation not allowed
    }

    public static void copyFileUsingByteChannel(Path sourcePath, Path destinationPath) throws IOException {
        Set<? extends OpenOption> options = Collections.emptySet();
        FileSystemProvider fileSystemProvider = sourcePath.getFileSystem().provider();
        try (FileChannel sourceFileChannel = fileSystemProvider.newFileChannel(sourcePath, options)) {
            long sourceFileSize = Files.size(sourcePath);
            try (WritableByteChannel writableByteChannel = Channels.newChannel(Files.newOutputStream(destinationPath))) {
                long transferredSize = 0;
                long bytesReadNow;
                while ((bytesReadNow = sourceFileChannel.transferTo(transferredSize, sourceFileSize - transferredSize, writableByteChannel)) > 0) {
                    transferredSize += bytesReadNow;
                }
            }
        }
    }

    public static void copyFileUsingFileChannel(Path sourcePath, String destinationLocalFilePath) throws IOException {
        Set<? extends OpenOption> options = Collections.emptySet();
        FileSystemProvider fileSystemProvider = sourcePath.getFileSystem().provider();
        try (FileChannel sourceFileChannel = fileSystemProvider.newFileChannel(sourcePath, options)) {
            long sourceFileSize = Files.size(sourcePath);
            try (FileOutputStream fileOutputStream = new FileOutputStream(destinationLocalFilePath, false)) {
                FileChannel destinationFileChannel = fileOutputStream.getChannel();
                long transferredSize = 0;
                long bytesReadNow;
                while ((bytesReadNow = sourceFileChannel.transferTo(transferredSize, sourceFileSize - transferredSize, destinationFileChannel)) > 0) {
                    transferredSize += bytesReadNow;
                }
            }
        }
    }

    public static void copyFileUsingInputStream(Path sourcePath, String destinationLocalFilePath) throws IOException {
        try (InputStream inputStream = sourcePath.getFileSystem().provider().newInputStream(sourcePath);
             ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
             FileOutputStream fileOutputStream = new FileOutputStream(destinationLocalFilePath, false)) {

            FileChannel destinationFileChannel = fileOutputStream.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            while ((readableByteChannel.read(buffer)) > 0) {
                // prepare the buffer to be drained
                buffer.flip();
                // write to the channel, may block
                destinationFileChannel.write(buffer);
                // if partial transfer, shift remainder down; if buffer is empty, same as doing clear()
                buffer.compact();
            }
            // EOF will leave buffer in fill state
            buffer.flip();
            // make sure the buffer is fully drained.
            while (buffer.hasRemaining()) {
                destinationFileChannel.write(buffer);
            }
        }
    }
}