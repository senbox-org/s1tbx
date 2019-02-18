package org.esa.snap.core.dataio.vfs.remote.object_storage.http;

import org.esa.snap.core.dataio.vfs.remote.object_storage.ObjectStorageFileSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Test: File System for HTTP Object Storage VFS.
 *
 * @author Adrian Drăghici
 */
public abstract class HttpFileSystemTest {

    private ObjectStorageFileSystem fs;

    abstract String getAddress();

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
        HttpFileSystemProvider.setupConnectionData(getAddress(), getUser(), getPassword());
        FileSystem fs = HttpFileSystemProvider.getHttpFileSystem();
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
        assertEquals("/IK2_OPER_OSA_GEO_1P_20080715T105300_N43-318_E003-351_0001.SIP/", iterator.next().toString());
        assertTrue(iterator.hasNext());
        assertEquals("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/", iterator.next().toString());
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
        SeekableByteChannel channel1 = provider.newByteChannel(fs.getPath("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/banner_1.png"), openOptions);
        SeekableByteChannel channel2 = provider.newByteChannel(fs.getPath("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/banner_1.png"), openOptions);
        SeekableByteChannel channel3 = provider.newByteChannel(fs.getPath("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/banner_1.png"), openOptions);
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
        Path path = fs.getPath("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/banner_1.png");
        HashSet<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        SeekableByteChannel channel = provider.newByteChannel(path, openOptions);

        assertNotNull(channel);
        assertEquals(42377, channel.size());
        assertEquals(0, channel.position());

        ByteBuffer buffer = ByteBuffer.wrap(new byte[42377]);
        int numRead = channel.read(buffer);
        assertEquals(42377, numRead);
        assertEquals(42377, channel.size());
        assertEquals(42377, channel.position());

        channel.position(30000);
        assertEquals(30000, channel.position());
        assertEquals(42377, channel.size());

        buffer = ByteBuffer.wrap(new byte[10000]);
        numRead = channel.read(buffer);
        assertEquals(10000, numRead);
        assertEquals(40000, channel.position());
        assertEquals(42377, channel.size());

        buffer = ByteBuffer.wrap(new byte[2377]);
        numRead = channel.read(buffer);
        assertEquals(2377, numRead);
        assertEquals(42377, channel.position());
        assertEquals(42377, channel.size());

        buffer = ByteBuffer.wrap(new byte[10]);
        try {
            numRead = channel.read(buffer);
            fail("EOFException expected, but read " + numRead + " bytes");
        } catch (EOFException e) {
            // ok
        }
    }

    @Test
    public void testBasicFileAttributes() throws Exception {
        if (!isReady()) {
            return;
        }
        Path path = fs.getPath("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/banner_1.png");
        assertEquals(42377, Files.size(path));
        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
        assertNotNull(lastModifiedTime);
    }

    @Test
    public void testPathsGet() {
        if (!isReady()) {
            return;
        }
        Path path = fs.getPath("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/banner_1.png");
        assertNotNull(path);
        assertEquals("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/banner_1.png", path.toString());
    }

    @Test
    public void testFilesWalk() throws Exception {
        if (!isReady()) {
            return;
        }
        Path path = fs.getPath("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/");
        Iterator<Path> iterator = Files.walk(path).iterator();
        while (iterator.hasNext()) {
            Path next = iterator.next();
            System.out.println("next = " + next + ", abs=" + path.isAbsolute());
        }
    }

    @Test
    @Ignore
    public void testWrite() throws Exception {
        if (!isReady()) {
            return;
        }
        FileSystemProvider provider = fs.provider();
        Path pathSrc = fs.getPath("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/banner_1.png");
        HashSet<OpenOption> openOptionsSrc = new HashSet<>();
        openOptionsSrc.add(StandardOpenOption.READ);
        SeekableByteChannel channelSrc = provider.newByteChannel(pathSrc, openOptionsSrc);
        ByteBuffer bufferSrc = ByteBuffer.wrap(new byte[42377]);
        int numRead = channelSrc.read(bufferSrc);
        assertEquals(42377, numRead);
        assertEquals(42377, channelSrc.size());
        assertEquals(42377, channelSrc.position());

        Path pathDest = fs.getPath("/S2A_MSIL2A_20170628T092031_N0205_R093_T34TGQ_20170628T092026.SAFE/HTML/banner_1_new.png");
        HashSet<OpenOption> openOptionsDest = new HashSet<>();
        openOptionsDest.add(StandardOpenOption.WRITE);
        SeekableByteChannel channelDest = provider.newByteChannel(pathDest, openOptionsDest);
        int numWrite = channelDest.write(bufferSrc);
        assertEquals(42377, numWrite);
    }

}
