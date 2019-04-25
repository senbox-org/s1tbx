package org.esa.snap.vfs.remote.http;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Test: File System for HTTP VFS.
 *
 * @author Adrian Drăghici
 */
public class HttpFileSystemTest extends AbstractVFSTest {

    private static final String TEST_DIR = "mock-api/vfs/";

    private AbstractRemoteFileSystem httpFileSystem;
    private HttpMockService mockService;

    private String getAddress() {
        return getHTTPRepo().getAddress();
    }

    @Before
    public void setUpHttpFileSystemTest() {
        try {
            VFSRemoteFileRepository httpRepo = getHTTPRepo();
            assumeNotNull(httpRepo);
            FileSystemProvider fileSystemProvider = VFS.getInstance().getFileSystemProviderByScheme(httpRepo.getScheme());
            assumeNotNull(fileSystemProvider);
            assumeTrue(fileSystemProvider instanceof AbstractRemoteFileSystemProvider);
            URI uri = new URI(httpRepo.getScheme(), httpRepo.getRoot(), null);
            FileSystem fs = fileSystemProvider.getFileSystem(uri);
            assumeNotNull(fs);
            httpFileSystem = (AbstractRemoteFileSystem) fs;
            Path serviceRootPath = vfsTestsFolderPath.resolve(TEST_DIR);
            assumeTrue(Files.exists(serviceRootPath));
            mockService = new HttpMockService(new URL(httpRepo.getAddress()), serviceRootPath);
            mockService.start();
        } catch (Exception e) {
            fail("Test requirements not meets.");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (httpFileSystem != null) {
            httpFileSystem.close();
        }
        if (mockService != null) {
            mockService.stop();
        }
    }

    @Test
    public void testScanner() throws Exception {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();

        HttpFileSystemProvider fileSystemProvider = (HttpFileSystemProvider) VFS.getInstance().getFileSystemProviderByScheme(httpRepo.getScheme());

        List<BasicFileAttributes> items;

        HttpWalker walker = new HttpWalker(getAddress(), "/", httpRepo.getRoot(), fileSystemProvider);
        items = walker.walk(NioPaths.get(httpRepo.getRoot() + "/"));
        assertEquals(2, items.size());

        walker = new HttpWalker(getAddress(), "/", httpRepo.getRoot(), fileSystemProvider);
        items = walker.walk(NioPaths.get(httpRepo.getRoot() + "/rootDir1/"));
        assertEquals(2, items.size());

        walker = new HttpWalker(getAddress(), "/", httpRepo.getRoot(), fileSystemProvider);
        items = walker.walk(NioPaths.get(httpRepo.getRoot() + "/rootDir1/dir1/"));
        assertEquals(2, items.size());
    }

    @Test
    public void testGET() throws Exception {
        String address = getAddress();
        if (address.endsWith("/")) {
            address = address.substring(0, address.lastIndexOf("/"));
        }
        URL url = new URL(address + "/rootDir1/dir1/file.jpg");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setDoOutput(true);
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
        assertEquals("/", httpFileSystem.getSeparator());
    }

    @Test
    public void testGetRootDirectories() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        Iterable<Path> rootDirectories = httpFileSystem.getRootDirectories();
        Iterator<Path> iterator = rootDirectories.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(httpRepo.getRoot() + "/rootDir1/", iterator.next().toString());
        assertTrue(iterator.hasNext());
        assertEquals(httpRepo.getRoot() + "/rootDir2/", iterator.next().toString());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testClose() throws Exception {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        FileSystemProvider provider = httpFileSystem.provider();
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel1 = provider.newByteChannel(httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions);
        SeekableByteChannel channel2 = provider.newByteChannel(httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions);
        SeekableByteChannel channel3 = provider.newByteChannel(httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions);
        assertTrue(httpFileSystem.isOpen());
        assertTrue(channel1.isOpen());
        assertTrue(channel2.isOpen());
        assertTrue(channel3.isOpen());
        httpFileSystem.close();
        assertFalse(httpFileSystem.isOpen());
        assertFalse(channel1.isOpen());
        assertFalse(channel2.isOpen());
        assertFalse(channel3.isOpen());
    }

    @Test
    public void testByteChannel() throws Exception {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        FileSystemProvider provider = httpFileSystem.provider();
        Path path = httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg");
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
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        Path path = httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg");
        assertEquals(1891, Files.size(path));
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        assertNotNull(lastModifiedTime);
    }

    @Test
    public void testPathsGet() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        Path path = httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg");
        assertNotNull(path);
        assertEquals(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg", path.toString());
    }

    @Test
    public void testFilesWalk() throws Exception {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        Path path = httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/");
        Iterator<Path> iterator = Files.walk(path).iterator();
        assertTrue(iterator.hasNext());
        Path next = iterator.next();
        assertEquals(httpRepo.getRoot() + "/rootDir1/", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(httpRepo.getRoot() + "/rootDir1/dir1/", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(httpRepo.getRoot() + "/rootDir1/dir1/subDir/", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(httpRepo.getRoot() + "/rootDir1/dir2/", next.toString());
        assertTrue(next.isAbsolute());
    }

}
