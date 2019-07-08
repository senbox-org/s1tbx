package org.esa.snap.vfs.remote.swift;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.esa.snap.vfs.NioPaths;
import org.esa.snap.vfs.VFS;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.esa.snap.vfs.remote.AbstractVFSTest;
import org.esa.snap.vfs.remote.VFSPath;
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Test: Remote File System for OpenStack Swift VFS.
 *
 * @author Adrian Drăghici
 */
public class SwiftFileSystemTest extends AbstractVFSTest {

    private static final String TEST_DIR = "mock-api/";

    private AbstractRemoteFileSystem swiftFileSystem;
    private SwiftMockService mockService;
    private SwiftAuthMockService authMockService;


    private String getAddress() {
        if (this.mockService != null) {
            return this.mockService.getMockServiceAddress();
        } else {
            return getSwiftRepo().getAddress();
        }
    }

    private String getContainer() {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        String container = "";
        if (!swiftRepo.getProperties().isEmpty()) {
            container = swiftRepo.getProperties().get(0).getValue();
        }
        return container;
    }

    private String getAuthAddress() {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        String authAddress = "";
        if (!swiftRepo.getProperties().isEmpty()) {
            authAddress = swiftRepo.getProperties().get(1).getValue();
        }
        return authAddress;
    }

    private String getDomain() {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        String domain = "";
        if (!swiftRepo.getProperties().isEmpty()) {
            domain = swiftRepo.getProperties().get(2).getValue();
        }
        return domain;
    }

    private String getProjectId() {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        String projectId = "";
        if (!swiftRepo.getProperties().isEmpty()) {
            projectId = swiftRepo.getProperties().get(3).getValue();
        }
        return projectId;
    }

    private String getUser() {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        String user = "";
        if (!swiftRepo.getProperties().isEmpty()) {
            user = swiftRepo.getProperties().get(4).getValue();
        }
        return user;
    }

    private String getPassword() {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        String password = "";
        if (!swiftRepo.getProperties().isEmpty()) {
            password = swiftRepo.getProperties().get(5).getValue();
        }
        return password;
    }

    @Before
    public void setUpSwiftFileSystemTest() {
        try {
            VFSRemoteFileRepository swiftRepo = getSwiftRepo();
            assertNotNull(swiftRepo);
            Path serviceRootPath = this.vfsTestsFolderPath.resolve(TEST_DIR);
            this.mockService = new SwiftMockService(new URL(swiftRepo.getAddress()), serviceRootPath);
            this.authMockService = new SwiftAuthMockService(new URL(getAuthAddress()));
            FileSystemProvider fileSystemProvider = VFS.getInstance().getFileSystemProviderByScheme(swiftRepo.getScheme());
            assertNotNull(fileSystemProvider);
            assumeTrue(fileSystemProvider instanceof AbstractRemoteFileSystemProvider);
            Map<String, String> connectionData = new LinkedHashMap<>();
            connectionData.put("authAddress", this.authMockService.getMockServiceAddress());//override 'authAddress' Swift property with Swift Auth Mock Service address
            ((AbstractRemoteFileSystemProvider) fileSystemProvider).setConnectionData(swiftRepo.getRoot(), this.mockService.getMockServiceAddress(), connectionData);
            URI uri = new URI(swiftRepo.getScheme(), swiftRepo.getRoot(), null);
            FileSystem fs = fileSystemProvider.getFileSystem(uri);
            assertNotNull(fs);
            this.swiftFileSystem = (AbstractRemoteFileSystem) fs;
            assumeTrue(Files.exists(serviceRootPath));
            this.mockService.start();
            this.authMockService.start();
        } catch (Exception e) {
            Logger.getLogger(SwiftFileSystemTest.class.getName()).log(Level.WARNING, "Testing requirements are not met. " + e.getMessage() + "\n" + ExceptionUtils.getFullStackTrace(e));
            assumeTrue(false);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (this.swiftFileSystem != null) {
            this.swiftFileSystem.close();
        }
        if (this.mockService != null) {
            this.mockService.stop();
        }
        if (this.authMockService != null) {
            this.authMockService.stop();
        }
    }

    @Test
    public void testScanner() throws Exception {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();

        SwiftFileSystemProvider fileSystemProvider = (SwiftFileSystemProvider) VFS.getInstance().getFileSystemProviderByScheme(swiftRepo.getScheme());

        List<BasicFileAttributes> items;

        SwiftWalker walker = new SwiftWalker(getAddress(), getContainer(), "/", swiftRepo.getRoot(), fileSystemProvider);
        items = walker.walk(VFSPath.toRemotePath(NioPaths.get(swiftRepo.getRoot() + "")));
        assertEquals(2, items.size());

        walker = new SwiftWalker(getAddress(), getContainer(), "/", swiftRepo.getRoot(), fileSystemProvider);
        items = walker.walk(VFSPath.toRemotePath(NioPaths.get(swiftRepo.getRoot() + "/rootDir1/")));
        assertEquals(2, items.size());

        walker = new SwiftWalker(getAddress(), getContainer(), "/", swiftRepo.getRoot(), fileSystemProvider);
        items = walker.walk(VFSPath.toRemotePath(NioPaths.get(swiftRepo.getRoot() + "/rootDir1/dir1/")));
        assertEquals(2, items.size());
    }

    @Test
    public void testGET() throws Exception {
        String address = getAddress();
        if (!address.endsWith("/")) {
            address = address.concat("/");
        }
        URL url = new URL(address + getContainer() + "/rootDir1/dir1/file.jpg");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        SwiftAuthenticationV3 swiftAuthenticationV3 = new SwiftAuthenticationV3(this.authMockService.getMockServiceAddress(), getDomain(), getProjectId(), getUser(), getPassword());
        connection.setRequestProperty("X-Auth-Token", swiftAuthenticationV3.getAuthorizationToken());

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
        assertEquals("/", this.swiftFileSystem.getSeparator());
    }

    @Test
    public void testGetRootDirectories() {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        String expectedPaths = swiftRepo.getRoot() + "/rootDir1/\n" + swiftRepo.getRoot() + "/rootDir2/";
        Iterable<Path> rootDirectories = this.swiftFileSystem.getRootDirectories();
        Iterator<Path> iterator = rootDirectories.iterator();
        assertTrue(iterator.hasNext());
        assertTrue(expectedPaths.contains(iterator.next().toString()));
        assertTrue(iterator.hasNext());
        assertTrue(expectedPaths.contains(iterator.next().toString()));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testClose() throws Exception {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        FileSystemProvider provider = this.swiftFileSystem.provider();
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        try (
                SeekableByteChannel channel1 = provider.newByteChannel(this.swiftFileSystem.getPath(swiftRepo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions);
                SeekableByteChannel channel2 = provider.newByteChannel(this.swiftFileSystem.getPath(swiftRepo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions);
                SeekableByteChannel channel3 = provider.newByteChannel(this.swiftFileSystem.getPath(swiftRepo.getRoot() + "/rootDir1/dir1/file.jpg"), openOptions)
        ) {
            assertTrue(this.swiftFileSystem.isOpen());
            assertTrue(channel1.isOpen());
            assertTrue(channel2.isOpen());
            assertTrue(channel3.isOpen());
            this.swiftFileSystem.close();
            assertFalse(this.swiftFileSystem.isOpen());
            assertFalse(channel1.isOpen());
            assertFalse(channel2.isOpen());
            assertFalse(channel3.isOpen());
        }
    }

    @Test
    public void testByteChannel() throws Exception {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        FileSystemProvider provider = this.swiftFileSystem.provider();
        Path path = this.swiftFileSystem.getPath(swiftRepo.getRoot() + "/rootDir1/dir1/file.jpg");
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
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        Path path = this.swiftFileSystem.getPath(swiftRepo.getRoot() + "/rootDir1/dir1/file.jpg");
        assertEquals(1891, Files.size(path));
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        assertNotNull(lastModifiedTime);
    }

    @Test
    public void testPathsGet() {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        Path path = this.swiftFileSystem.getPath(swiftRepo.getRoot() + "/rootDir1/dir1/file.jpg");
        assertNotNull(path);
        assertEquals(swiftRepo.getRoot() + "/rootDir1/dir1/file.jpg", path.toString());
    }

    @Test
    public void testFilesWalk() throws Exception {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        String expectedPaths = swiftRepo.getRoot() + "/rootDir1/\n" + swiftRepo.getRoot() + "/rootDir1/dir1/\n" + swiftRepo.getRoot() + "/rootDir1/dir1/file.jpg\n" + swiftRepo.getRoot() + "/rootDir1/dir1/subDir/" + swiftRepo.getRoot() + "/rootDir1/dir2/";
        Path path = this.swiftFileSystem.getPath(swiftRepo.getRoot() + "/rootDir1/");
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
