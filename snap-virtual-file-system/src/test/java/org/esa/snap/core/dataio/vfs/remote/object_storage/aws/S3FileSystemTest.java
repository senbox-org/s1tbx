package org.esa.snap.core.dataio.vfs.remote.object_storage.aws;

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
 * Test: File System for S3 Object Storage VFS.
 * Requirements:
 * - A file with name: creds_s3.txt, located in user directory.
 * The file must contains the following:
 * - Address of S3 service
 * - Access Key ID
 * - Secret Access Key
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public abstract class S3FileSystemTest {

    private static Logger logger = Logger.getLogger(S3FileSystemTest.class.getName());

    private ObjectStorageFileSystem fs;

    abstract String getBucketAddress();

    abstract String getAccessKeyId();

    abstract String getSecretAccessKey();

    abstract String getDirForTest();

    abstract Long getDirForTestItems();

    abstract String getFileForTest();

    abstract Long getFileForTestSize();

    abstract void setData();

    abstract boolean isReady();


    @Before
    public void setUp() throws Exception {
        setData();
        if (!isReady()) {
            return;
        }
        FileSystem fs = S3FileSystemProvider.getS3FileSystem(getBucketAddress(), getAccessKeyId(), getSecretAccessKey());
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
        assertEquals("/S3/.git/", iterator.next().toString());
        assertTrue(iterator.hasNext());
        assertEquals("/S3/css/", iterator.next().toString());
        assertTrue(iterator.hasNext());
        assertEquals("/S3/images/", iterator.next().toString());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testClose() throws Exception {
        if (!isReady()) {
            return;
        }
        FileSystemProvider provider = fs.provider();
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel1 = provider.newByteChannel(fs.getPath("/images/rpi.svg"), openOptions);
        SeekableByteChannel channel2 = provider.newByteChannel(fs.getPath("/images/rpi.svg"), openOptions);
        SeekableByteChannel channel3 = provider.newByteChannel(fs.getPath("/images/rpi.svg"), openOptions);
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
        Path path = fs.getPath("/images/rpi.svg");
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
        if (!isReady()) {
            return;
        }
        Path path = fs.getPath("/images/rpi.svg");
        assertEquals(6489, Files.size(path));
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        assertNotNull(lastModifiedTime);
    }

    @Test
    public void testPathsGet() {
        if (!isReady()) {
            return;
        }
        Path path = fs.getPath("/images/rpi.svg");
        assertNotNull(path);
        assertEquals("/images/rpi.svg", path.toString());
    }

    @Test
    public void testFilesWalk() throws Exception {
        if (!isReady()) {
            return;
        }
        Path path = fs.getPath("/images/");
        Iterator<Path> iterator = Files.walk(path).iterator();
        while (iterator.hasNext()) {
            Path next = iterator.next();
            System.out.println("next = " + next + ", abs=" + path.isAbsolute());
        }
    }

}
