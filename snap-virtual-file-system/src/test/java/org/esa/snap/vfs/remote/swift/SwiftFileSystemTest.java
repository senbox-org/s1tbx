package org.esa.snap.vfs.remote.swift;

import org.esa.snap.vfs.NioPaths;
import org.esa.snap.vfs.VFS;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Test: Remote File System for OpenStack Swift VFS.
 *
 * @author Adrian Drăghici
 */
public class SwiftFileSystemTest extends AbstractVFSTest {

    private static final String TEST_DIR = "mock-api/";

    private static AbstractRemoteFileSystem swiftFileSystem;
    private SwiftMockService mockService;
    private SwiftAuthMockService authMockService;


    private String getAddress() {
        return getSwiftRepo().getAddress();
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
            FileSystemProvider fileSystemProvider = VFS.getInstance().getFileSystemProviderByScheme(swiftRepo.getScheme());
            assertNotNull(fileSystemProvider);
            assumeTrue(fileSystemProvider instanceof AbstractRemoteFileSystemProvider);
            URI uri = new URI(swiftRepo.getScheme(), swiftRepo.getRoot(), null);
            FileSystem fs = fileSystemProvider.newFileSystem(uri, null);
            assertNotNull(fs);
            swiftFileSystem = (AbstractRemoteFileSystem) fs;
            Path serviceRootPath = vfsTestsFolderPath.resolve(TEST_DIR);
            assumeTrue(Files.exists(serviceRootPath));
            mockService = new SwiftMockService(new URL(swiftRepo.getAddress()), serviceRootPath);
            mockService.start();
            authMockService = new SwiftAuthMockService(new URL(getAuthAddress()));
            authMockService.start();
        } catch (Exception e) {
            fail("Test requirements not meets.");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (swiftFileSystem != null) {
            swiftFileSystem.close();
        }
        if (mockService != null) {
            mockService.stop();
        }
        if (authMockService != null) {
            authMockService.stop();
        }
    }

    @Test
    public void testScanner() throws Exception {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        List<BasicFileAttributes> items;

        //TODO Jean replace null with an object
        SwiftWalker walker = new SwiftWalker(getAddress(), getContainer(), "/", swiftRepo.getRoot(), null);
        items = walker.walk(NioPaths.get(swiftRepo.getRoot() + ""));
        assertEquals(2, items.size());

        walker = new SwiftWalker(getAddress(), getContainer(), "/", swiftRepo.getRoot(), null);
        items = walker.walk(NioPaths.get(swiftRepo.getRoot() + "/rootDir1/"));
        assertEquals(2, items.size());

        walker = new SwiftWalker(getAddress(), getContainer(), "/", swiftRepo.getRoot(), null);
        items = walker.walk(NioPaths.get(swiftRepo.getRoot() + "/rootDir1/dir1/"));
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
        connection.setRequestProperty("X-Auth-Token", SwiftAuthMockService.TOKEN);

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
        assertEquals("/", swiftFileSystem.getSeparator());
    }

    @Test
    public void testGetRootDirectories() {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        Iterable<Path> rootDirectories = swiftFileSystem.getRootDirectories();
        Iterator<Path> iterator = rootDirectories.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1", iterator.next().toString());
        assertTrue(iterator.hasNext());
        assertEquals(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir2", iterator.next().toString());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testClose() throws Exception {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        FileSystemProvider provider = swiftFileSystem.provider();
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel1 = provider.newByteChannel(swiftFileSystem.getPath(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir1/file.jpg"), openOptions);
        SeekableByteChannel channel2 = provider.newByteChannel(swiftFileSystem.getPath(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir1/file.jpg"), openOptions);
        SeekableByteChannel channel3 = provider.newByteChannel(swiftFileSystem.getPath(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir1/file.jpg"), openOptions);
        assertTrue(swiftFileSystem.isOpen());
        assertTrue(channel1.isOpen());
        assertTrue(channel2.isOpen());
        assertTrue(channel3.isOpen());
        swiftFileSystem.close();
        assertFalse(swiftFileSystem.isOpen());
        assertFalse(channel1.isOpen());
        assertFalse(channel2.isOpen());
        assertFalse(channel3.isOpen());
    }

    @Test
    public void testByteChannel() throws Exception {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        FileSystemProvider provider = swiftFileSystem.provider();
        Path path = swiftFileSystem.getPath(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir1/file.jpg");
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
        try {
            numRead = channel.read(buffer);
            fail("EOFException expected, but read " + numRead + " bytes");
        } catch (EOFException ex) {
            //ok
        }
    }

    @Test
    public void testBasicFileAttributes() throws Exception {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        Path path = swiftFileSystem.getPath(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir1/file.jpg");
        assertEquals(1891, Files.size(path));
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        assertNotNull(lastModifiedTime);
    }

    @Test
    public void testPathsGet() {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        Path path = swiftFileSystem.getPath(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir1/file.jpg");
        assertNotNull(path);
        assertEquals(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir1/file.jpg", path.toString());
    }

    @Test
    public void testFilesWalk() throws Exception {
        VFSRemoteFileRepository swiftRepo = getSwiftRepo();
        Path path = swiftFileSystem.getPath(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/");
        Iterator<Path> iterator = Files.walk(path).iterator();
        assertTrue(iterator.hasNext());
        Path next = iterator.next();
        assertEquals(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir1", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir1/file.jpg", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir1/subDir", next.toString());
        assertTrue(next.isAbsolute());
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertEquals(swiftRepo.getRoot() + "/" + getContainer() + "/rootDir1/dir2", next.toString());
        assertTrue(next.isAbsolute());
    }

}
