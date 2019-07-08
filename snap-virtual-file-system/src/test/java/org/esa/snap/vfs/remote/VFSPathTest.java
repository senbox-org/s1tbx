package org.esa.snap.vfs.remote;

import org.apache.commons.lang.exception.ExceptionUtils;
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
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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

    private AbstractRemoteFileSystem vfs;
    private HttpMockService mockService;


    @Before
    public void setUpObjectStoragePathTest() {
        try {
            VFSRemoteFileRepository httpRepo = getHTTPRepo();
            assumeNotNull(httpRepo);
            Path serviceRootPath = this.vfsTestsFolderPath.resolve(TEST_DIR);
            this.mockService = new HttpMockService(new URL(httpRepo.getAddress()), serviceRootPath);
            FileSystemProvider fileSystemProvider = VFS.getInstance().getFileSystemProviderByScheme(httpRepo.getScheme());
            assumeNotNull(fileSystemProvider);
            assumeTrue(fileSystemProvider instanceof AbstractRemoteFileSystemProvider);
            ((AbstractRemoteFileSystemProvider) fileSystemProvider).setConnectionData(httpRepo.getRoot(), this.mockService.getMockServiceAddress(), new LinkedHashMap<>());
            URI uri = new URI(httpRepo.getScheme(), httpRepo.getRoot(), null);
            FileSystem fs = fileSystemProvider.getFileSystem(uri);
            assumeNotNull(fs);
            this.vfs = (AbstractRemoteFileSystem) fs;
            assumeTrue(Files.exists(serviceRootPath));
            this.mockService.start();
        } catch (Exception e) {
            Logger.getLogger(VFSPathTest.class.getName()).log(Level.WARNING, "Testing requirements are not met. " + e.getMessage() + "\n" + ExceptionUtils.getFullStackTrace(e));
            assumeTrue(false);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (this.vfs != null) {
            this.vfs.close();
        }
        if (this.mockService != null) {
            this.mockService.stop();
        }
    }

    @Test
    public void testGetPath() throws URISyntaxException {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        URI pathUri = new URI(this.vfs.provider().getScheme(), httpRepo.getRoot() + "/hello", null);
        Path path = this.vfs.provider().getPath(pathUri);
        assertEquals(httpRepo.getRoot() + "/hello", path.toString());
    }

    @Test
    public void testGetNameCount() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        VFSPath path;

        path = VFSPath.parsePath(this.vfs, "");
        assertEquals(0, path.getNameCount());

        path = VFSPath.parsePath(this.vfs, "/");
        assertEquals(0, path.getNameCount());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + "/hello");
        assertEquals(2, path.getNameCount());

        path = VFSPath.parsePath(this.vfs, "hello/there");
        assertEquals(2, path.getNameCount());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + "/hello/there");
        assertEquals(3, path.getNameCount());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + "/hello/there/");
        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testIsAbsolute() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        VFSPath path;

        path = VFSPath.parsePath(this.vfs, "");
        assertFalse(path.isAbsolute());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + '/');
        assertTrue(path.isAbsolute());

        path = VFSPath.parsePath(this.vfs, "/hello/there");
        assertFalse(path.isAbsolute());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + "/hello/there/");
        assertTrue(path.isAbsolute());
    }

    @Test
    public void testGetRoot() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        VFSPath path;

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + '/');
        assertSame(this.vfs.getRoot(), path.getRoot());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + "/hello/there");
        assertSame(this.vfs.getRoot(), path.getRoot());

        path = VFSPath.parsePath(this.vfs, "");
        assertNull(path.getRoot());

        path = VFSPath.parsePath(this.vfs, "hello/there");
        assertNull(path.getRoot());
    }

    @Test
    public void testGetFileName() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        VFSPath path;

        path = VFSPath.parsePath(this.vfs, "");
        assertNull(path.getFileName());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + '/');
        assertEquals(httpRepo.getRoot(), path.getFileName().toString());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + "/hello/there");
        assertEquals("there", path.getFileName().toString());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + "hello/there/");
        assertEquals("there", path.getFileName().toString());
    }

    @Test
    public void testRelativize() {
        Path path1 = VFSPath.parsePath(this.vfs, "/a/b");
        Path path2 = VFSPath.parsePath(this.vfs, "/a/b/c/d");
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
        Path p = VFSPath.parsePath(this.vfs, sp);
        Path q = VFSPath.parsePath(this.vfs, sq);
        Path r = p.relativize(p.resolve(q));
        assertEquals(q.toString(), r.toString());
    }

    @Test
    public void testResolve() {
        Path path;

        path = VFSPath.parsePath(this.vfs, "");
        assertEquals("", path.resolve("").toString());
        assertEquals("", path.resolve("/").toString());
        assertEquals("/gus", path.resolve("gus").toString());
        assertEquals("/gus", path.resolve("gus/").toString());
        assertEquals("/gus", path.resolve("/gus/").toString());

        path = VFSPath.parsePath(this.vfs, "/");
        assertEquals("", path.resolve("").toString());
        assertEquals("", path.resolve("/").toString());
        assertEquals("/gus", path.resolve("gus").toString());
        assertEquals("/gus", path.resolve("gus/").toString());
        assertEquals("/gus", path.resolve("/gus/").toString());

        path = VFSPath.parsePath(this.vfs, "foo/bar/");
        assertEquals("foo/bar", path.resolve("").toString());
        assertEquals("foo/bar", path.resolve("/").toString());
        assertEquals("foo/bar/gus", path.resolve("gus").toString());
        assertEquals("foo/bar/gus", path.resolve("gus/").toString());
        assertEquals("foo/bar/gus", path.resolve("/gus/").toString());

        path = VFSPath.parsePath(this.vfs, "/foo/bar/");
        assertEquals("/foo/bar", path.resolve("").toString());
        assertEquals("/foo/bar", path.resolve("/").toString());
        assertEquals("/foo/bar/gus", path.resolve("gus").toString());
        assertEquals("/foo/bar/gus", path.resolve("gus/").toString());
        assertEquals("/foo/bar/gus", path.resolve("/gus/").toString());

        path = VFSPath.parsePath(this.vfs, "foo/bar");
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

        path = VFSPath.parsePath(this.vfs, "");
        assertEquals("", path.resolveSibling("").toString());
        assertEquals("", path.resolveSibling("/").toString());
        assertEquals("gus", path.resolveSibling("gus").toString());
        assertEquals("gus", path.resolveSibling("gus/").toString());
        assertEquals("/gus", path.resolveSibling("/gus/").toString());

        path = VFSPath.parsePath(this.vfs, "/");
        assertEquals("", path.resolveSibling("").toString());
        assertEquals("/Test:", path.resolveSibling("/Test:/").toString());
        assertEquals("gus", path.resolveSibling("gus").toString());
        assertEquals("gus", path.resolveSibling("gus/").toString());
        assertEquals("/gus", path.resolveSibling("/gus/").toString());

        path = VFSPath.parsePath(this.vfs, "foo/bar/");
        assertEquals("foo/bar", path.resolveSibling("").toString());
        assertEquals("foo/gus", path.resolveSibling("gus").toString());
        assertEquals("foo/gus", path.resolveSibling("gus/").toString());
        assertEquals("foo/gus", path.resolveSibling("/gus/").toString());

        path = VFSPath.parsePath(this.vfs, "/foo/bar/");
        assertEquals("/foo/bar", path.resolveSibling("").toString());
        assertEquals("/foo/gus", path.resolveSibling("gus").toString());
        assertEquals("/foo/gus", path.resolveSibling("gus/").toString());
        assertEquals("/foo/gus", path.resolveSibling("/gus/").toString());
    }

    @Test
    public void testGetParent() {
        VFSRemoteFileRepository httpRepo = getHTTPRepo();
        VFSPath path;

        path = VFSPath.parsePath(this.vfs, "");
        assertNull(path.getParent());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + '/');
        assertNull(path.getParent());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + "/hello");
        assertEquals(httpRepo.getRoot(), path.getParent().toString());

        path = VFSPath.parsePath(this.vfs, "hello/");
        assertNull(path.getParent());

        path = VFSPath.parsePath(this.vfs, httpRepo.getRoot() + "/hello/there");
        assertEquals(httpRepo.getRoot() + "/hello", path.getParent().toString());

        path = VFSPath.parsePath(this.vfs, "hello/there/");
        assertEquals("hello", path.getParent().toString());
    }

    @Test
    public void testToString() {
        VFSPath path;

        path = VFSPath.parsePath(this.vfs, "");
        assertEquals("", path.toString());

        path = VFSPath.parsePath(this.vfs, "/");
        assertEquals("", path.toString());

        path = VFSPath.parsePath(this.vfs, "hello");
        assertEquals("hello", path.toString());

        path = VFSPath.parsePath(this.vfs, "hello/");
        assertEquals("hello", path.toString());

        path = VFSPath.parsePath(this.vfs, "/hello");
        assertEquals("/hello", path.toString());

        path = VFSPath.parsePath(this.vfs, "/hello/");
        assertEquals("/hello", path.toString());

        path = VFSPath.parsePath(this.vfs, "/hello/there");
        assertEquals("/hello/there", path.toString());

        path = VFSPath.parsePath(this.vfs, "/hello/there/");
        assertEquals("/hello/there", path.toString());
    }

    @Test
    public void testNormalize() {
        VFSPath path;
        Path normalizedPath;

        path = VFSPath.parsePath(this.vfs, "");
        normalizedPath = path.normalize();
        assertEquals("", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, ".");
        normalizedPath = path.normalize();
        assertEquals("", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "./");
        normalizedPath = path.normalize();
        assertEquals("", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "./hello");
        normalizedPath = path.normalize();
        assertEquals("hello", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "./hello/");
        normalizedPath = path.normalize();
        assertEquals("hello", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "./hello/.");
        normalizedPath = path.normalize();
        assertEquals("hello", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "./hello/./");
        normalizedPath = path.normalize();
        assertEquals("hello", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "./hello/..");
        normalizedPath = path.normalize();
        assertEquals("", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "./hello/../");
        normalizedPath = path.normalize();
        assertEquals("", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "./hello/there/../");
        normalizedPath = path.normalize();
        assertEquals("hello", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "./hello/there/../..");
        normalizedPath = path.normalize();
        assertEquals("", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "hello/there/../..");
        normalizedPath = path.normalize();
        assertEquals("", normalizedPath.toString());

        path = VFSPath.parsePath(this.vfs, "hello/there/../../");
        normalizedPath = path.normalize();
        assertEquals("", normalizedPath.toString());

    }
}
