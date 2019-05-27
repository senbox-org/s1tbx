package org.esa.snap.vfs.remote;

import org.esa.snap.vfs.VFS;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.http.HttpMockService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Test: Path for VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class VFSPathTest extends AbstractVFSTest {

    private static final String TEST_DIR = "mock-api/vfs/";

    private AbstractRemoteFileSystem fs;
    private HttpMockService mockService;


    @Before
    public void setUpObjectStoragePathTest() {
        try {
            VFSRemoteFileRepository httpRepo = getHTTPRepo();
            assumeNotNull(httpRepo);
            FileSystemProvider fileSystemProvider = VFS.getInstance().getFileSystemProviderByScheme(httpRepo.getScheme());
            assumeNotNull(fileSystemProvider);
            assumeTrue(fileSystemProvider instanceof AbstractRemoteFileSystemProvider);
            URI uri = new URI(httpRepo.getScheme(), httpRepo.getRoot(), null);
            FileSystem fs = fileSystemProvider.getFileSystem(uri);
            assumeNotNull(fs);
            this.fs = (AbstractRemoteFileSystem) fs;
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
        if (fs != null) {
            fs.close();
        }
        if (mockService != null) {
            mockService.stop();
        }
    }

    @Test
    public void testGetPath() throws URISyntaxException {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        URI pathUri = new URI(fs.provider().getScheme(), httpRepo.getRoot() + "/hello", null);
        Path path = fs.provider().getPath(pathUri);
        assertEquals(httpRepo.getRoot() + "/hello", path.toString());
    }

    @Test
    public void testGetNameCount() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        VFSPath path;

        path = VFSPath.parsePath(fs, "");
        assertEquals(0, path.getNameCount());

        path = VFSPath.parsePath(fs, "/");
        assertEquals(0, path.getNameCount());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/hello");
        assertEquals(2, path.getNameCount());

        path = VFSPath.parsePath(fs, "hello/there");
        assertEquals(2, path.getNameCount());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/hello/there");
        assertEquals(3, path.getNameCount());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/hello/there/");
        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testIsAbsolute() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        VFSPath path;

        path = VFSPath.parsePath(fs, "");
        assertFalse(path.isAbsolute());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/");
        assertTrue(path.isAbsolute());

        path = VFSPath.parsePath(fs, "/hello/there");
        assertFalse(path.isAbsolute());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/hello/there/");
        assertTrue(path.isAbsolute());
    }

    @Test
    public void testGetRoot() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        VFSPath path;

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/");
        assertSame(fs.getRoot(), path.getRoot());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/hello/there");
        assertSame(fs.getRoot(), path.getRoot());

        path = VFSPath.parsePath(fs, "");
        assertNull(path.getRoot());

        path = VFSPath.parsePath(fs, "hello/there");
        assertNull(path.getRoot());
    }

    @Test
    public void testGetFileName() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        VFSPath path;

        path = VFSPath.parsePath(fs, "");
        assertNull(path.getFileName());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/");
        assertEquals(httpRepo.getRoot(), path.getFileName().toString());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/hello/there");
        assertEquals("there", path.getFileName().toString());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "hello/there/");
        assertEquals("there", path.getFileName().toString());
    }

    @Test
    public void testRelativize() {
        Path path1, path2;

        path1 = VFSPath.parsePath(fs, "/a/b");
        path2 = VFSPath.parsePath(fs, "/a/b/c/d");
        assertEquals("", path1.relativize(path1).toString());
        assertEquals("", path2.relativize(path2).toString());
        assertEquals("c/d", path1.relativize(path2).toString());
        assertEquals("/a/b", path2.relativize(path1).toString());
    }

    @Test
    public void testRelativizeIsInverseOfResolve() {
        assertRelativizeIsInverseOfResolve("/foo/bar/", "");
        assertRelativizeIsInverseOfResolve("/foo/bar/", "baz");
        assertRelativizeIsInverseOfResolve("/foo/bare/", "baz/booz");
    }

    private void assertRelativizeIsInverseOfResolve(String sp, String sq) {
        // For any two normalized paths p and q, where q does not have a root component, p.relativize(p.resolve(q)).equals(q).
        Path p = VFSPath.parsePath(fs, sp);
        Path q = VFSPath.parsePath(fs, sq);
        Path r = p.relativize(p.resolve(q));
        assertEquals(q.toString(), r.toString());
    }

    @Test
    public void testResolve() {
        Path path;

        path = VFSPath.parsePath(fs, "");
        assertEquals("", path.resolve("").toString());
        assertEquals("/", path.resolve("/").toString());
        assertEquals("/gus", path.resolve("gus").toString());
        assertEquals("/gus/", path.resolve("gus/").toString());
        assertEquals("/gus/", path.resolve("/gus/").toString());

        path = VFSPath.parsePath(fs, "/");
        assertEquals("/", path.resolve("").toString());
        assertEquals("/", path.resolve("/").toString());
        assertEquals("/gus", path.resolve("gus").toString());
        assertEquals("/gus/", path.resolve("gus/").toString());
        assertEquals("/gus/", path.resolve("/gus/").toString());

        path = VFSPath.parsePath(fs, "foo/bar/");
        assertEquals("foo/bar/", path.resolve("").toString());
        assertEquals("foo/bar/", path.resolve("/").toString());
        assertEquals("foo/bar/gus", path.resolve("gus").toString());
        assertEquals("foo/bar/gus/", path.resolve("gus/").toString());
        assertEquals("foo/bar/gus/", path.resolve("/gus/").toString());

        path = VFSPath.parsePath(fs, "/foo/bar/");
        assertEquals("/foo/bar/", path.resolve("").toString());
        assertEquals("/foo/bar/", path.resolve("/").toString());
        assertEquals("/foo/bar/gus", path.resolve("gus").toString());
        assertEquals("/foo/bar/gus/", path.resolve("gus/").toString());
        assertEquals("/foo/bar/gus/", path.resolve("/gus/").toString());

        path = VFSPath.parsePath(fs, "foo/bar");
        try {
            path.resolve((String) null);
            Assert.fail("IllegalArgumentException expected");
        } catch (NullPointerException ignored) {
            //ok
        }
    }

    @Test
    public void testResolveSibling() {
        Path path;

        path = VFSPath.parsePath(fs, "");
        assertEquals("", path.resolveSibling("").toString());
        assertEquals("/", path.resolveSibling("/").toString());
        assertEquals("gus", path.resolveSibling("gus").toString());
        assertEquals("gus/", path.resolveSibling("gus/").toString());
        assertEquals("/gus/", path.resolveSibling("/gus/").toString());

        path = VFSPath.parsePath(fs, "/");
        assertEquals("/", path.resolveSibling("").toString());
        assertEquals("/Test:/", path.resolveSibling("/Test:/").toString());
        assertEquals("gus", path.resolveSibling("gus").toString());
        assertEquals("gus/", path.resolveSibling("gus/").toString());
        assertEquals("/gus/", path.resolveSibling("/gus/").toString());

        path = VFSPath.parsePath(fs, "foo/bar/");
        assertEquals("foo/bar/", path.resolveSibling("").toString());
        assertEquals("foo/", path.resolveSibling("/").toString());
        assertEquals("foo/gus", path.resolveSibling("gus").toString());
        assertEquals("foo/gus/", path.resolveSibling("gus/").toString());
        assertEquals("foo/gus/", path.resolveSibling("/gus/").toString());

        path = VFSPath.parsePath(fs, "/foo/bar/");
        assertEquals("/foo/bar/", path.resolveSibling("").toString());
        assertEquals("/foo/", path.resolveSibling("/").toString());
        assertEquals("/foo/gus", path.resolveSibling("gus").toString());
        assertEquals("/foo/gus/", path.resolveSibling("gus/").toString());
        assertEquals("/foo/gus/", path.resolveSibling("/gus/").toString());
    }

    @Test
    public void testGetParent() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        VFSPath path;

        path = VFSPath.parsePath(fs, "");
        assertNull(path.getParent());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/");
        assertNull(path.getParent());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/hello");
        assertEquals(httpRepo.getRoot(), path.getParent().toString());

        path = VFSPath.parsePath(fs, "hello/");
        assertNull(path.getParent());

        path = VFSPath.parsePath(fs, httpRepo.getRoot() + "/hello/there");
        assertEquals(httpRepo.getRoot() + "/hello", path.getParent().toString());

        path = VFSPath.parsePath(fs, "hello/there/");
        assertEquals("hello", path.getParent().toString());
    }

    @Test
    public void testToString() {
        VFSPath path;

        path = VFSPath.parsePath(fs, "");
        assertEquals("", path.toString());

        path = VFSPath.parsePath(fs, "/");
        assertEquals("/", path.toString());

        path = VFSPath.parsePath(fs, "hello");
        assertEquals("hello", path.toString());

        path = VFSPath.parsePath(fs, "hello/");
        assertEquals("hello/", path.toString());

        path = VFSPath.parsePath(fs, "/hello");
        assertEquals("/hello", path.toString());

        path = VFSPath.parsePath(fs, "/hello/");
        assertEquals("/hello/", path.toString());

        path = VFSPath.parsePath(fs, "/hello/there");
        assertEquals("/hello/there", path.toString());

        path = VFSPath.parsePath(fs, "/hello/there/");
        assertEquals("/hello/there/", path.toString());
    }
}
