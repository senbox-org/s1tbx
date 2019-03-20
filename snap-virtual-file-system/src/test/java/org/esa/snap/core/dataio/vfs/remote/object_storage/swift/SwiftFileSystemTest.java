package org.esa.snap.core.dataio.vfs.remote.object_storage.swift;

import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Test: Remote File System for OpenStack Swift Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
public abstract class SwiftFileSystemTest {

    private static Logger logger = Logger.getLogger(SwiftFileSystemTest.class.getName());

    private ObjectStorageFileSystem fs;

    abstract String getAddress();

    abstract String getAuthAddress();

    abstract String getContainer();

    abstract String getDomain();

    abstract String getProjectId();

    abstract String getUser();

    abstract String getPassword();

    abstract void setCredentials();

    abstract boolean isReady();


    @Before
    public void setUp() throws Exception {
        setCredentials();
        if (!isReady()) {
            return;
        }
        FileSystem fs = SwiftFileSystemProvider.getSwiftFileSystem(getAddress(), getAuthAddress(), getContainer(), getDomain(), getProjectId(), getUser(), getPassword());
        assertNotNull(fs);
        assertTrue(fs instanceof ObjectStorageFileSystem);
        this.fs = (ObjectStorageFileSystem) fs;
    }

    @After
    public void tearDown() throws Exception {
        if (!isReady()) {
            return;
        }
        assertNotNull(fs);
        fs.close();
    }

    @Test
    public void testSeparator() {
        if (!isReady()) {
            return;
        }
        assertEquals("/", fs.getSeparator());
    }

    @Test
    public void testGetRootDirectories() {
        if (!isReady()) {
            return;
        }
        Iterable<Path> rootDirectories = fs.getRootDirectories();
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
        if (!isReady()) {
            return;
        }
        FileSystemProvider provider = fs.provider();
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel1 = provider.newByteChannel(fs.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg"), openOptions);
        SeekableByteChannel channel2 = provider.newByteChannel(fs.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg"), openOptions);
        SeekableByteChannel channel3 = provider.newByteChannel(fs.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg"), openOptions);
        assertTrue(fs.isOpen());
        assertTrue(channel1.isOpen());
        assertTrue(channel2.isOpen());
        assertTrue(channel3.isOpen());
        fs.close();
        assertFalse(fs.isOpen());
        assertFalse(channel1.isOpen());
        assertFalse(channel2.isOpen());
        assertFalse(channel3.isOpen());
    }

    @Test
    public void testByteChannel() throws Exception {
        if (!isReady()) {
            return;
        }
        FileSystemProvider provider = fs.provider();
        Path path = fs.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg");
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
        if (!isReady()) {
            return;
        }
        Path path = fs.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg");
        assertEquals(113963, Files.size(path));
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        assertNotNull(lastModifiedTime);
    }

    @Test
    public void testPathsGet() {
        if (!isReady()) {
            return;
        }
        Path path = fs.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg");
        assertNotNull(path);
        assertEquals("/romania/LC08_L2A_180029_20161010_20170320_01_T1/mosaic.jpg", path.toString());
    }

    @Test
    public void testFilesWalk() throws Exception {
        if (!isReady()) {
            return;
        }
        Path path = fs.getPath("/romania/LC08_L2A_180029_20161010_20170320_01_T1/");
        Iterator<Path> iterator = Files.walk(path).iterator();
        while (iterator.hasNext()) {
            Path next = iterator.next();
            System.out.println("next = " + next + ", abs=" + path.isAbsolute());
        }
    }

}
