package org.esa.snap.vfs.remote;

import org.esa.snap.vfs.remote.http.HttpFileSystemProvider;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Created by jcoravu on 5/4/2019.
 */
public class TransferFileContentMain {

    public static void main(String[] args) throws Exception {
        System.out.println("TransferFileContentMain");

        String url = "vfs:/snap-products/files-to-test.zip";
        String localFile = "D:/_test-extract-zip/files-to-test-downloaded.zip";

//        String url = "vfs:/snap-products/_rapideye/Somalia_Mod.zip";
//        String localFile = "D:/_test-extract-zip/Somalia_Mod-downloaded.zip";

//        String url = "vfs:/snap-products/PL1_OPER_HIR_P_S_3__20140225T143800_N13-908_W060-960_4011.SIP.ZIP";
//        String localFile = "D:/_test-extract-zip/PL1_OPER_HIR_P_S_3__20140225T143800_N13-908_W060-960_4011.SIP.ZIP";

        HttpFileSystemProvider httpFileSystemProvider = new HttpFileSystemProvider();
        Map<String, ?> connectionData = Collections.emptyMap();
        httpFileSystemProvider.setConnectionData("http://localhost", connectionData);
        URI uri = new URI("http", url, null);
        Path path = httpFileSystemProvider.getPath(uri);

//        Path path = Paths.get("C:\\Apache24\\htdocs\\snap-products\\files-to-test.zip");
//        Path path = Paths.get("C:\\Apache24\\htdocs\\snap-products\\_rapideye\\Somalia_Mod.zip");
//        Path path = Paths.get("C:\\Apache24\\htdocs\\snap-products\\PL1_OPER_HIR_P_S_3__20140225T143800_N13-908_W060-960_4011.SIP.ZIP");

//        copyFileUsingFileChannel(path, localFile);
        Path localPath = Paths.get(localFile);
        copyFileUsingByteChannel(path, localPath);

//        boolean downloadComplete = false;
//        while (!downloadComplete) {
//            downloadComplete = transferData(path, localFile);
//        }

        System.out.println("  Finished");
    }

    public static void copyFileUsingByteChannel(Path sourcePath, Path destinationPath) throws IOException {
        Set<? extends OpenOption> options = Collections.emptySet();
        FileSystemProvider fileSystemProvider = sourcePath.getFileSystem().provider();
        try (FileChannel sourceFileChannel = fileSystemProvider.newFileChannel(sourcePath, options)) {
            long sourceFileSize = Files.size(sourcePath);
            try (WritableByteChannel wbc = Channels.newChannel(Files.newOutputStream(destinationPath))) {
                long transferredSize = 0;
                long delta;
                while ((delta = sourceFileChannel.transferTo(transferredSize, sourceFileSize - transferredSize, wbc)) > 0) {
                    transferredSize += delta;
                    System.out.println(transferredSize + " bytes received " + " delta=" + delta);
                }
            }
        }
    }
    public static void copyFileUsingFileChannel(Path sourcePath, String destinationLocalFilePath) throws IOException {
        Set<? extends OpenOption> options = Collections.emptySet();
        FileSystemProvider fileSystemProvider = sourcePath.getFileSystem().provider();
        try (FileChannel sourceFileChannel = fileSystemProvider.newFileChannel(sourcePath, options)) {
            long sourceFileSize = Files.size(sourcePath);
            try (FileOutputStream fos = new FileOutputStream(destinationLocalFilePath, false)) {
                long transferredSize = 0;
                long delta;
                while ((delta = sourceFileChannel.transferTo(transferredSize, sourceFileSize - transferredSize, fos.getChannel())) > 0) {
                    transferredSize += delta;
                    System.out.println(transferredSize + " bytes received " + " delta=" + delta);
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
            int bytesReadNow;
            while ((bytesReadNow = readableByteChannel.read(buffer)) > 0) {
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