package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.NioPaths;
import org.esa.snap.vfs.VFS;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractVFSTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.EOFException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test: File System for S3 Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class S3FileSystemTest extends AbstractVFSTest {

    private static Logger logger = Logger.getLogger(S3FileSystemTest.class.getName());

    private static AbstractRemoteFileSystem s3FileSystem;
    private static VFSRemoteFileRepository s3Repo;

    private static String getBucketAddress() {
        return s3Repo.getAddress();
    }

    private static String getAccessKeyId() {
        return s3Repo.getProperties().get(0).getValue();
    }

    private static String getSecretAccessKey() {
        return s3Repo.getProperties().get(1).getValue();
    }


    @Before
    public void setUpS3FileSystemTest() {
        s3Repo = vfsRepositories.get(0);
        assertNotNull(s3Repo);
        FileSystemProvider fileSystemProvider = VFS.getInstance().getFileSystemProviderByScheme(s3Repo.getScheme());
        assertNotNull(fileSystemProvider);
        FileSystem fs = null;
        try {
            URI uri = new URI(s3Repo.getScheme() + ":" + s3Repo.getAddress());
            fs = fileSystemProvider.newFileSystem(uri, null);
        } catch (Exception ignored) {
            //do nothing
        }
        assertNotNull(fs);
        assertTrue(fs instanceof AbstractRemoteFileSystem);
        s3FileSystem = (AbstractRemoteFileSystem) fs;
    }

    @After
    public void tearDown() throws Exception {
        assertNotNull(s3FileSystem);
        s3FileSystem.close();
    }

    @Test
    public void testScanner() throws Exception {
        List<BasicFileAttributes> items;
        items = new S3Walker(getBucketAddress(), getAccessKeyId(), getSecretAccessKey(), "/", "").walk(NioPaths.get(""));
        assertEquals(11, items.size());

        items = new S3Walker(getBucketAddress(), getAccessKeyId(), getSecretAccessKey(), "/", "").walk(NioPaths.get(".git/"));
        assertEquals(11, items.size());

        items = new S3Walker(getBucketAddress(), getAccessKeyId(), getSecretAccessKey(), "/", "").walk(NioPaths.get("css/"));
        assertEquals(2, items.size());
    }

    @Test
    public void testGET() throws Exception {
        URL url = new URL(getBucketAddress() + "/images/rpi.svg");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.connect();

        int responseCode = connection.getResponseCode();
        System.out.println("responseCode = " + responseCode);
        String responseMessage = connection.getResponseMessage();
        System.out.println("responseMessage = " + responseMessage);

        InputStream stream = connection.getInputStream();
        byte[] b = new byte[1024 * 1024];
        int read = stream.read(b);
        assertTrue(read > 0);
        ReadableByteChannel channel = Channels.newChannel(stream);
        channel.close();
        connection.disconnect();
    }

    @Test
    public void testSeparator() {
        assertEquals("/", s3FileSystem.getSeparator());
    }

    @Test
    public void testGetRootDirectories() {
        Iterable<Path> rootDirectories = s3FileSystem.getRootDirectories();
        Iterator<Path> iterator = rootDirectories.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("/S3/.git/", iterator.next().toString());
        assertTrue(iterator.hasNext());
        assertEquals("/S3/css/", iterator.next().toString());
        assertTrue(iterator.hasNext());
        assertEquals("/S3/images/", iterator.next().toString());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testClose() throws Exception {
        FileSystemProvider provider = s3FileSystem.provider();
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel1 = provider.newByteChannel(s3FileSystem.getPath("/images/rpi.svg"), openOptions);
        SeekableByteChannel channel2 = provider.newByteChannel(s3FileSystem.getPath("/images/rpi.svg"), openOptions);
        SeekableByteChannel channel3 = provider.newByteChannel(s3FileSystem.getPath("/images/rpi.svg"), openOptions);
        assertTrue(s3FileSystem.isOpen());
        assertTrue(channel1.isOpen());
        assertTrue(channel2.isOpen());
        assertTrue(channel3.isOpen());
        s3FileSystem.close();
        assertFalse(s3FileSystem.isOpen());
        assertFalse(channel1.isOpen());
        assertFalse(channel2.isOpen());
        assertFalse(channel3.isOpen());
    }

    @Test
    public void testByteChannel() throws Exception {
        FileSystemProvider provider = s3FileSystem.provider();
        Path path = s3FileSystem.getPath("/images/rpi.svg");
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel = provider.newByteChannel(path, openOptions);

        assertNotNull(channel);
        assertEquals(6489, channel.size());
        assertEquals(0, channel.position());

        ByteBuffer buffer = ByteBuffer.wrap(new byte[6489]);
        int numRead = channel.read(buffer);
        assertEquals(6489, numRead);
        assertEquals(6489, channel.size());
        assertEquals(6489, channel.position());

        channel.position(5000);
        assertEquals(5000, channel.position());
        assertEquals(6489, channel.size());

        buffer = ByteBuffer.wrap(new byte[1000]);
        numRead = channel.read(buffer);
        assertEquals(1000, numRead);
        assertEquals(6000, channel.position());
        assertEquals(6489, channel.size());

        buffer = ByteBuffer.wrap(new byte[489]);
        numRead = channel.read(buffer);
        assertEquals(489, numRead);
        assertEquals(6489, channel.position());
        assertEquals(6489, channel.size());

        buffer = ByteBuffer.wrap(new byte[10]);
        try {
            numRead = channel.read(buffer);
            fail("EOFException expected, but read " + numRead + " bytes");
        } catch (EOFException ex) {
            logger.log(Level.SEVERE, "Unable to run test for Byte Channel. Details: " + ex.getMessage());
        }
    }

    @Test
    public void testBasicFileAttributes() throws Exception {
        Path path = s3FileSystem.getPath("/images/rpi.svg");
        assertEquals(6489, Files.size(path));
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        assertNotNull(lastModifiedTime);
    }

    @Test
    public void testPathsGet() {
        Path path = s3FileSystem.getPath("/images/rpi.svg");
        assertNotNull(path);
        assertEquals("/images/rpi.svg", path.toString());
    }

    @Test
    public void testFilesWalk() throws Exception {
        Path path = s3FileSystem.getPath("/images/");
        Iterator<Path> iterator = Files.walk(path).iterator();
        while (iterator.hasNext()) {
            Path next = iterator.next();
            System.out.println("next = " + next + ", abs=" + path.isAbsolute());
        }
    }

}
