package org.esa.snap.vfs.remote.http;

import org.apache.commons.lang.exception.ExceptionUtils;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
        if (this.mockService != null) {
            return this.mockService.getMockServiceAddress();
        } else {
            return getHTTPRepo().getAddress();
        }
    }

    @Before
    public void setUpHttpFileSystemTest() {
        try {
            VFSRemoteFileRepository httpRepo = getHTTPRepo();
            assumeNotNull(httpRepo);
            Path serviceRootPath = this.vfsTestsFolderPath.resolve(TEST_DIR);
            this.mockService = new HttpMockService(new URL(httpRepo.getAddress()), serviceRootPath);
            FileSystemProvider fileSystemProvider = VFS.getInstance().getFileSystemProviderByScheme(httpRepo.getScheme());
            assumeNotNull(fileSystemProvider);
            assumeTrue(fileSystemProvider instanceof AbstractRemoteFileSystemProvider);
            ((AbstractRemoteFileSystemProvider) fileSystemProvider).setConnectionData(this.mockService.getMockServiceAddress(), new LinkedHashMap<>());
            URI uri = new URI(httpRepo.getScheme(), httpRepo.getRoot(), null);
            FileSystem fs = fileSystemProvider.getFileSystem(uri);
            assumeNotNull(fs);
            this.httpFileSystem = (AbstractRemoteFileSystem) fs;
            assumeTrue(Files.exists(serviceRootPath));
            this.mockService.start();
        } catch (Exception e) {
            Logger.getLogger(HttpFileSystemTest.class.getName()).log(Level.WARNING, "Testing requirements are not met. " + e.getMessage() + "\n" + ExceptionUtils.getFullStackTrace(e));
            assumeTrue(false);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (this.httpFileSystem != null) {
            this.httpFileSystem.close();
        }
        if (this.mockService != null) {
            this.mockService.stop();
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
            address = address.substring(0, address.lastIndexOf('/'));
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
        assertEquals("/", this.httpFileSystem.getSeparator());
    }

    @Test
    public void testGetRootDirectories() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        String expectedPaths = httpRepo.getRoot() + "/rootDir1/\n" + httpRepo.getRoot() + "/rootDir2/";
        Iterable<Path> rootDirectories = this.httpFileSystem.getRootDirectories();
        Iterator<Path> iterator = rootDirectories.iterator();
        assertTrue(iterator.hasNext());
        assertTrue(expectedPaths.contains(iterator.next().toString()));
        assertTrue(iterator.hasNext());
        assertTrue(expectedPaths.contains(iterator.next().toString()));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testClose() throws Exception {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        FileSystemProvider provider = this.httpFileSystem.provider();
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        try (
                SeekableByteChannel channel1 = provider.newByteChannel(this.httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions);
                SeekableByteChannel channel2 = provider.newByteChannel(this.httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions);
                SeekableByteChannel channel3 = provider.newByteChannel(this.httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions)
        ) {
            assertTrue(this.httpFileSystem.isOpen());
            assertTrue(channel1.isOpen());
            assertTrue(channel2.isOpen());
            assertTrue(channel3.isOpen());
            this.httpFileSystem.close();
            assertFalse(this.httpFileSystem.isOpen());
            assertFalse(channel1.isOpen());
            assertFalse(channel2.isOpen());
            assertFalse(channel3.isOpen());
        }
    }

    @Test
    public void testByteChannel() throws Exception {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        FileSystemProvider provider = this.httpFileSystem.provider();
        Path path = this.httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg");
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
        Path path = this.httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg");
        assertEquals(1891, Files.size(path));
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        assertNotNull(lastModifiedTime);
    }

    @Test
    public void testPathsGet() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        Path path = this.httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg");
        assertNotNull(path);
        assertEquals(httpRepo.getRoot() + "/rootDir1/dir1/file.jpg", path.toString());
    }

    @Test
    public void testFilesWalk() throws Exception {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        String expectedPaths = httpRepo.getRoot() + "/rootDir1/\n" + httpRepo.getRoot() + "/rootDir1/dir1/\n" + httpRepo.getRoot() + "/rootDir1/dir1/file.jpg\n" + httpRepo.getRoot() + "/rootDir1/dir1/subDir/" + httpRepo.getRoot() + "/rootDir1/dir2/";
        Path path = this.httpFileSystem.getPath(httpRepo.getRoot() + "/rootDir1/");
        try (Stream<Path> iteratorStream = Files.walk(path)) {
            Iterator<Path> iterator = iteratorStream.iterator();
            assertTrue(iterator.hasNext());
            Path next = iterator.next();
            assertTrue(expectedPaths.contains(next.toString()));
            assertTrue(next.isAbsolute());
            assertTrue(iterator.hasNext());
            next = iterator.next();
            assertTrue(expectedPaths.contains(next.toString()));
            assertTrue(next.isAbsolute());
            assertTrue(iterator.hasNext());
            next = iterator.next();
            assertTrue(expectedPaths.contains(next.toString()));
            assertTrue(next.isAbsolute());
            assertTrue(iterator.hasNext());
            next = iterator.next();
            assertTrue(expectedPaths.contains(next.toString()));
            assertTrue(next.isAbsolute());
            assertTrue(iterator.hasNext());
            next = iterator.next();
            assertTrue(expectedPaths.contains(next.toString()));
            assertTrue(next.isAbsolute());
        }
    }

}
