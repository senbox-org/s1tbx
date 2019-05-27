package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.NioPaths;
import org.esa.snap.vfs.VFS;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.esa.snap.vfs.remote.AbstractVFSTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Test: File System for S3 VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class S3FileSystemTest extends AbstractVFSTest {

    private static final String TEST_DIR = "mock-api/vfs/";

    private static AbstractRemoteFileSystem s3FileSystem;
    private S3MockService mockService;

    private String getBucketAddress() {
        return getS3Repo().getAddress();
    }

    @Before
    public void setUpS3FileSystemTest() {
        try {
            VFSRemoteFileRepository s3Repo = getS3Repo();
            assertNotNull(s3Repo);
            FileSystemProvider fileSystemProvider = VFS.getInstance().getFileSystemProviderByScheme(s3Repo.getScheme());
            assertNotNull(fileSystemProvider);
            assumeTrue(fileSystemProvider instanceof AbstractRemoteFileSystemProvider);
            URI uri = new URI(s3Repo.getScheme(), s3Repo.getRoot(), null);
            FileSystem fs = fileSystemProvider.getFileSystem(uri);
            assertNotNull(fs);
            s3FileSystem = (AbstractRemoteFileSystem) fs;
            Path serviceRootPath = vfsTestsFolderPath.resolve(TEST_DIR);
            assumeTrue(Files.exists(serviceRootPath));
            mockService = new S3MockService(new URL(s3Repo.getAddress()), serviceRootPath);
            mockService.start();
        } catch (Exception e) {
            fail("Test requirements not meets.");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (s3FileSystem != null) {
            s3FileSystem.close();
        }
        if (mockService != null) {
            mockService.stop();
        }
    }

    @Test
    public void testScanner() throws Exception {
        VFSRemoteFileRepository s3Repo = getS3Repo();

        S3FileSystemProvider fileSystemProvider = (S3FileSystemProvider) VFS.getInstance().getFileSystemProviderByScheme(s3Repo.getScheme());

        List<BasicFileAttributes> items;

        S3Walker walker = new S3Walker(getBucketAddress(), "/", s3Repo.getRoot(), fileSystemProvider);
        items = walker.walk(NioPaths.get(s3Repo.getRoot() + ""));
        assertEquals(2, items.size());

        walker = new S3Walker(getBucketAddress(), "/", s3Repo.getRoot(), fileSystemProvider);
        items = walker.walk(NioPaths.get(s3Repo.getRoot() + "/rootDir1/"));
        assertEquals(2, items.size());

        walker = new S3Walker(getBucketAddress(), "/", s3Repo.getRoot(), fileSystemProvider);
        items = walker.walk(NioPaths.get(s3Repo.getRoot() + "/rootDir1/dir1/"));
        assertEquals(2, items.size());
    }

    @Test
    public void testGET() throws Exception {
        String address = getBucketAddress();
        if (address.endsWith("/")) {
            address = address.substring(0, address.lastIndexOf("/"));
        }
        URL url = new URL(address + "/rootDir1/dir1/file.jpg");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.connect();

        int responseCode = connection.getResponseCode();
        assertEquals(HttpURLConnection.HTTP_OK, responseCode);

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
        VFSRemoteFileRepository s3Repo = getS3Repo();
        Iterable<Path> rootDirectories = s3FileSystem.getRootDirectories();
        Iterator<Path> iterator = rootDirectories.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(s3Repo.getRoot() + "/rootDir1/", iterator.next().toString());
        assertTrue(iterator.hasNext());
        assertEquals(s3Repo.getRoot() + "/rootDir2/", iterator.next().toString());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testClose() throws Exception {
        VFSRemoteFileRepository s3Repo = getS3Repo();
        FileSystemProvider provider = s3FileSystem.provider();
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel1 = provider.newByteChannel(s3FileSystem.getPath(s3Repo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions);
        SeekableByteChannel channel2 = provider.newByteChannel(s3FileSystem.getPath(s3Repo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions);
        SeekableByteChannel channel3 = provider.newByteChannel(s3FileSystem.getPath(s3Repo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions);
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
        VFSRemoteFileRepository s3Repo = getS3Repo();
        FileSystemProvider provider = s3FileSystem.provider();
        Path path = s3FileSystem.getPath(s3Repo.getRoot() + "/rootDir1/dir1/file.jpg");
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel = provider.newByteChannel(path, openOptions);

        assertNotNull(channel);
        assertEquals(1891, channel.size());
        assertEquals(0, channel.position());

        ByteBuffer buffer = ByteBuffer.wrap(new byte[1891]);
        int numRead = channel.read(buffer);
        assertEquals(1891, numRead);
        assertEquals(1891, channel.size());
        assertEquals(1891, channel.position());

        channel.position(1000);
        assertEquals(1000, channel.position());
        assertEquals(1891, channel.size());

        buffer = ByteBuffer.wrap(new byte[800]);
        numRead = channel.read(buffer);
        assertEquals(800, numRead);
        assertEquals(1800, channel.position());
        assertEquals(1891, channel.size());

        buffer = ByteBuffer.wrap(new byte[91]);
        numRead = channel.read(buffer);
        assertEquals(91, numRead);
        assertEquals(1891, channel.position());
        assertEquals(1891, channel.size());

        buffer = ByteBuffer.wrap(new byte[10]);
        numRead = channel.read(buffer);
        assertEquals(-1, numRead);
    }

    @Test
    public void testBasicFileAttributes() throws Exception {
        VFSRemoteFileRepository s3Repo = getS3Repo();
        Path path = s3FileSystem.getPath(s3Repo.getRoot() + "/rootDir1/dir1/file.jpg");
        assertEquals(1891, Files.size(path));
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        assertNotNull(lastModifiedTime);
    }

    @Test
    public void testPathsGet() {
        VFSRemoteFileRepository s3Repo = getS3Repo();
        Path path = s3FileSystem.getPath(s3Repo.getRoot() + "/rootDir1/dir1/file.jpg");
        assertNotNull(path);
        assertEquals(s3Repo.getRoot() + "/rootDir1/dir1/file.jpg", path.toString());
    }

    @Test
    public void testFilesWalk() throws Exception {
        VFSRemoteFileRepository s3Repo = getS3Repo();
        Path path = s3FileSystem.getPath(s3Repo.getRoot() + "/rootDir1/");
        Iterator<Path> iterator = Files.walk(path).iterator();
        assertTrue(iterator.hasNext());
        Path next = iterator.next();
        assertEquals(s3Repo.getRoot() + "/rootDir1/", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(s3Repo.getRoot() + "/rootDir1/dir1/", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(s3Repo.getRoot() + "/rootDir1/dir1/file.jpg", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(s3Repo.getRoot() + "/rootDir1/dir1/subDir/", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(s3Repo.getRoot() + "/rootDir1/dir2/", next.toString());
        assertTrue(next.isAbsolute());
    }

}
