package org.esa.snap.vfs.remote.swift;

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
 * Test: Remote File System for OpenStack Swift Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
public class SwiftFileSystemTest extends AbstractVFSTest {

    private static Logger logger = Logger.getLogger(SwiftFileSystemTest.class.getName());

    private static AbstractRemoteFileSystem swiftFileSystem;
    private static VFSRemoteFileRepository swiftRepo;


    private static String getAddress() {
        return swiftRepo.getAddress();
    }

    private static String getAuthAddress() {
        return swiftRepo.getProperties().get(0).getValue();
    }

    private static String getContainer() {
        return swiftRepo.getProperties().get(1).getValue();
    }

    private static String getDomain() {
        return swiftRepo.getProperties().get(2).getValue();
    }

    private static String getProjectId() {
        return swiftRepo.getProperties().get(3).getValue();
    }

    private static String getUser() {
        return swiftRepo.getProperties().get(4).getValue();
    }

    private static String getPassword() {
        return swiftRepo.getProperties().get(5).getValue();
    }


    @Before
    public void setUpSwiftFileSystemTest() {
        swiftRepo = vfsRepositories.get(0);
        assertNotNull(swiftRepo);
        FileSystemProvider fileSystemProvider = VFS.getInstance().getFileSystemProviderByScheme(swiftRepo.getScheme());
        assertNotNull(fileSystemProvider);
        FileSystem fs = null;
        try {
            URI uri = new URI(swiftRepo.getScheme() + ":" + swiftRepo.getAddress());
            fs = fileSystemProvider.newFileSystem(uri, null);
        } catch (Exception ignored) {
            //do nothing
        }
        assertNotNull(fs);
        assertTrue(fs instanceof AbstractRemoteFileSystem);
        swiftFileSystem = (AbstractRemoteFileSystem) fs;
    }

    @After
    public void tearDown() throws Exception {
        assertNotNull(swiftFileSystem);
        swiftFileSystem.close();
    }

    @Test
    public void testScanner() throws Exception {
        List<BasicFileAttributes> items;

        items = new SwiftWalker(getAddress(), getAuthAddress(), getContainer(), getDomain(), getProjectId(), getUser(), getPassword(), "/", "").walk(NioPaths.get(""));
        assertEquals(8, items.size());

        items = new SwiftWalker(getAddress(), getAuthAddress(), getContainer(), getDomain(), getProjectId(), getUser(), getPassword(), "/", "").walk(NioPaths.get("romania/"));
        assertEquals(5267, items.size());

        items = new SwiftWalker(getAddress(), getAuthAddress(), getContainer(), getDomain(), getProjectId(), getUser(), getPassword(), "/", "").walk(NioPaths.get("romania/LC08_L2A_180029_20161010_20170320_01_T1/"));
        assertEquals(9, items.size());
    }

    @Test
    public void testGET() throws Exception {
        URL url = new URL(getAddress() + getContainer() + "/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg");
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
        assertEquals("/", swiftFileSystem.getSeparator());
    }

    @Test
    public void testGetRootDirectories() {
        Iterable<Path> rootDirectories = swiftFileSystem.getRootDirectories();
        Iterator<Path> iterator = rootDirectories.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("/OpenStack-Swift/C01/abc/", iterator.next().toString());
        assertTrue(iterator.hasNext());
        assertEquals("/OpenStack-Swift/C01/czechia/", iterator.next().toString());
        assertTrue(iterator.hasNext());
        assertEquals("/OpenStack-Swift/C01/italy/", iterator.next().toString());
        assertTrue(iterator.hasNext());
    }

    @Test
    public void testClose() throws Exception {
        FileSystemProvider provider = swiftFileSystem.provider();
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel1 = provider.newByteChannel(swiftFileSystem.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg"), openOptions);
        SeekableByteChannel channel2 = provider.newByteChannel(swiftFileSystem.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg"), openOptions);
        SeekableByteChannel channel3 = provider.newByteChannel(swiftFileSystem.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg"), openOptions);
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
        FileSystemProvider provider = swiftFileSystem.provider();
        Path path = swiftFileSystem.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg");
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel = provider.newByteChannel(path, openOptions);

        assertNotNull(channel);
        assertEquals(113963, channel.size());
        assertEquals(0, channel.position());

        ByteBuffer buffer = ByteBuffer.wrap(new byte[113963]);
        int numRead = channel.read(buffer);
        assertEquals(113963, numRead);
        assertEquals(113963, channel.size());
        assertEquals(113963, channel.position());

        channel.position(110000);
        assertEquals(110000, channel.position());
        assertEquals(113963, channel.size());

        buffer = ByteBuffer.wrap(new byte[3000]);
        numRead = channel.read(buffer);
        assertEquals(3000, numRead);
        assertEquals(113000, channel.position());
        assertEquals(113963, channel.size());

        buffer = ByteBuffer.wrap(new byte[963]);
        numRead = channel.read(buffer);
        assertEquals(963, numRead);
        assertEquals(113963, channel.position());
        assertEquals(113963, channel.size());

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
        Path path = swiftFileSystem.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg");
        assertEquals(113963, Files.size(path));
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        assertNotNull(lastModifiedTime);
    }

    @Test
    public void testPathsGet() {
        Path path = swiftFileSystem.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg");
        assertNotNull(path);
        assertEquals("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg", path.toString());
    }

    @Test
    public void testFilesWalk() throws Exception {
        Path path = swiftFileSystem.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/");
        Iterator<Path> iterator = Files.walk(path).iterator();
        while (iterator.hasNext()) {
            Path next = iterator.next();
            System.out.println("next = " + next + ", abs=" + path.isAbsolute());
        }
    }

}
