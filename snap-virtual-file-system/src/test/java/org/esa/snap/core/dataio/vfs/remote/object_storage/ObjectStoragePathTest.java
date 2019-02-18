package org.esa.snap.core.dataio.vfs.remote.object_storage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test: Path for Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class ObjectStoragePathTest {

    private static final String FS_ID = "test:" + ObjectStoragePathTest.class.getName();
    private static ObjectStorageFileSystem fs;

    @BeforeClass
    public static void setUp() throws Exception {
        Map<String, ?> env = new HashMap<>();
        FileSystem fs = FileSystems.newFileSystem(new URI(FS_ID), env);
        assertNotNull(fs);
        assertTrue(fs instanceof ObjectStorageFileSystem);
        ObjectStoragePathTest.fs = (ObjectStorageFileSystem) fs;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        fs.close();
    }

    @Test
    public void testGetNameCount() {
        ObjectStoragePath path;

        path = ObjectStoragePath.parsePath(fs, "");
        assertEquals(0, path.getNameCount());

        path = ObjectStoragePath.parsePath(fs, "/");
        assertEquals(0, path.getNameCount());

        path = ObjectStoragePath.parsePath(fs, "/hello");
        assertEquals(1, path.getNameCount());

        path = ObjectStoragePath.parsePath(fs, "hello/there");
        assertEquals(2, path.getNameCount());

        path = ObjectStoragePath.parsePath(fs, "/hello/there");
        assertEquals(2, path.getNameCount());

        path = ObjectStoragePath.parsePath(fs, "/hello/there/");
        assertEquals(2, path.getNameCount());
    }

    @Test
    public void testIsAbsolute() {
        ObjectStoragePath path;

        path = ObjectStoragePath.parsePath(fs, "");
        assertFalse(path.isAbsolute());

        path = ObjectStoragePath.parsePath(fs, "/");
        assertTrue(path.isAbsolute());

        path = ObjectStoragePath.parsePath(fs, "hello/there");
        assertFalse(path.isAbsolute());

        path = ObjectStoragePath.parsePath(fs, "/hello/there/");
        assertTrue(path.isAbsolute());
    }

    @Test
    public void testGetRoot() {
        ObjectStoragePath path;

        path = ObjectStoragePath.parsePath(fs, "/");
        assertSame(fs.getRoot(), path.getRoot());

        path = ObjectStoragePath.parsePath(fs, "/hello/there");
        assertSame(fs.getRoot(), path.getRoot());

        path = ObjectStoragePath.parsePath(fs, "");
        assertNull(path.getRoot());

        path = ObjectStoragePath.parsePath(fs, "hello/there");
        assertNull(path.getRoot());
    }

    @Test
    public void testGetFileName() {
        ObjectStoragePath path;

        path = ObjectStoragePath.parsePath(fs, "");
        assertNull(path.getFileName());

        path = ObjectStoragePath.parsePath(fs, "/");
        assertNull(path.getFileName());

        path = ObjectStoragePath.parsePath(fs, "/hello/there");
        assertEquals("there", path.getFileName().toString());

        path = ObjectStoragePath.parsePath(fs, "hello/there/");
        assertEquals("there/", path.getFileName().toString());
    }

    @Test
    public void testRelativize() {
        Path path1, path2;

        path1 = ObjectStoragePath.parsePath(fs, "/a/b");
        path2 = ObjectStoragePath.parsePath(fs, "/a/b/c/d");
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
        Path p = ObjectStoragePath.parsePath(fs, sp);
        Path q = ObjectStoragePath.parsePath(fs, sq);
        Path r = p.relativize(p.resolve(q));
        assertEquals(q, r);
    }

    @Test
    public void testResolve() {
        Path path;

        path = ObjectStoragePath.parsePath(fs, "");
        assertEquals("", path.resolve("").toString());
        assertEquals("/", path.resolve("/").toString());
        assertEquals("gus", path.resolve("gus").toString());
        assertEquals("gus/", path.resolve("gus/").toString());
        assertEquals("/gus/", path.resolve("/gus/").toString());

        path = ObjectStoragePath.parsePath(fs, "/");
        assertEquals("/", path.resolve("").toString());
        assertEquals("/", path.resolve("/").toString());
        assertEquals("/gus", path.resolve("gus").toString());
        assertEquals("/gus/", path.resolve("gus/").toString());
        assertEquals("/gus/", path.resolve("/gus/").toString());

        path = ObjectStoragePath.parsePath(fs, "foo/bar/");
        assertEquals("foo/bar/", path.resolve("").toString());
        assertEquals("/", path.resolve("/").toString());
        assertEquals("foo/bar/gus", path.resolve("gus").toString());
        assertEquals("foo/bar/gus/", path.resolve("gus/").toString());
        assertEquals("/gus/", path.resolve("/gus/").toString());

        path = ObjectStoragePath.parsePath(fs, "/foo/bar/");
        assertEquals("/foo/bar/", path.resolve("").toString());
        assertEquals("/", path.resolve("/").toString());
        assertEquals("/foo/bar/gus", path.resolve("gus").toString());
        assertEquals("/foo/bar/gus/", path.resolve("gus/").toString());
        assertEquals("/gus/", path.resolve("/gus/").toString());

        path = ObjectStoragePath.parsePath(fs, "foo/bar");
        try {
            path.resolve("gus");
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testResolveSibling() {
        Path path;

        path = ObjectStoragePath.parsePath(fs, "");
        assertEquals("", path.resolveSibling("").toString());
        assertEquals("/", path.resolveSibling("/").toString());
        assertEquals("gus", path.resolveSibling("gus").toString());
        assertEquals("gus/", path.resolveSibling("gus/").toString());
        assertEquals("/gus/", path.resolveSibling("/gus/").toString());

        path = ObjectStoragePath.parsePath(fs, "/");
        assertEquals("/", path.resolveSibling("").toString());
        assertEquals("/", path.resolveSibling("/").toString());
        assertEquals("gus", path.resolveSibling("gus").toString());
        assertEquals("gus/", path.resolveSibling("gus/").toString());
        assertEquals("/gus/", path.resolveSibling("/gus/").toString());

        path = ObjectStoragePath.parsePath(fs, "foo/bar/");
        assertEquals("foo/bar/", path.resolveSibling("").toString());
        assertEquals("/", path.resolveSibling("/").toString());
        assertEquals("foo/gus", path.resolveSibling("gus").toString());
        assertEquals("foo/gus/", path.resolveSibling("gus/").toString());
        assertEquals("/gus/", path.resolveSibling("/gus/").toString());

        path = ObjectStoragePath.parsePath(fs, "/foo/bar/");
        assertEquals("/foo/bar/", path.resolveSibling("").toString());
        assertEquals("/", path.resolveSibling("/").toString());
        assertEquals("/foo/gus", path.resolveSibling("gus").toString());
        assertEquals("/foo/gus/", path.resolveSibling("gus/").toString());
        assertEquals("/gus/", path.resolveSibling("/gus/").toString());
    }

    @Test
    public void testGetParent() {
        ObjectStoragePath path;

        path = ObjectStoragePath.parsePath(fs, "");
        assertNull(path.getParent());

        path = ObjectStoragePath.parsePath(fs, "/");
        assertNull(path.getParent());

        path = ObjectStoragePath.parsePath(fs, "/hello");
        assertEquals("/", path.getParent().toString());

        path = ObjectStoragePath.parsePath(fs, "hello/");
        assertEquals("", path.getParent().toString());

        path = ObjectStoragePath.parsePath(fs, "/hello/there");
        assertEquals("/hello/", path.getParent().toString());

        path = ObjectStoragePath.parsePath(fs, "hello/there/");
        assertEquals("hello/", path.getParent().toString());
    }

    @Test
    public void testToString() {
        ObjectStoragePath path;

        path = ObjectStoragePath.parsePath(fs, "");
        assertEquals("", path.toString());

        path = ObjectStoragePath.parsePath(fs, "/");
        assertEquals("/", path.toString());

        path = ObjectStoragePath.parsePath(fs, "hello");
        assertEquals("hello", path.toString());

        path = ObjectStoragePath.parsePath(fs, "hello/");
        assertEquals("hello/", path.toString());

        path = ObjectStoragePath.parsePath(fs, "/hello");
        assertEquals("/hello", path.toString());

        path = ObjectStoragePath.parsePath(fs, "/hello/");
        assertEquals("/hello/", path.toString());

        path = ObjectStoragePath.parsePath(fs, "/hello/there");
        assertEquals("/hello/there", path.toString());

        path = ObjectStoragePath.parsePath(fs, "/hello/there/");
        assertEquals("/hello/there/", path.toString());
    }

}
