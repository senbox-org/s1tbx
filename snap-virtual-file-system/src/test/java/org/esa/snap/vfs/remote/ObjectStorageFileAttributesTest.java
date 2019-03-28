package org.esa.snap.vfs.remote;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test: File Attributes for Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class ObjectStorageFileAttributesTest {

    @Test
    public void testFile() {
        LocalDateTime dateTime = LocalDateTime.parse("2018-10-17T17:24:10.009Z", ObjectStorageFileAttributes.ISO_DATE_TIME);

        BasicFileAttributes fileAttributes = ObjectStorageFileAttributes.newFile("index.html", 34986, "2018-10-17T17:24:10.009Z");
        assertEquals("index.html", fileAttributes.fileKey());
        assertTrue(fileAttributes.isRegularFile());
        assertFalse(fileAttributes.isDirectory());
        assertFalse(fileAttributes.isSymbolicLink());
        assertFalse(fileAttributes.isOther());
        assertEquals(34986, fileAttributes.size());
        assertEquals(FileTime.from(dateTime.toInstant(ZoneOffset.UTC)), fileAttributes.lastModifiedTime());
    }

    @Test
    public void testDir() {
        BasicFileAttributes fileAttributes = ObjectStorageFileAttributes.newDir("products/");
        assertEquals("products/", fileAttributes.fileKey());
        assertFalse(fileAttributes.isRegularFile());
        assertTrue(fileAttributes.isDirectory());
        assertFalse(fileAttributes.isSymbolicLink());
        assertFalse(fileAttributes.isOther());
        assertEquals(0, fileAttributes.size());
        Assert.assertEquals(ObjectStorageFileAttributes.UNKNOWN_FILE_TIME, fileAttributes.lastModifiedTime());
    }

    @Test
    public void testEmpty() {
        BasicFileAttributes fileAttributes = ObjectStorageFileAttributes.getEmpty();
        assertEquals("", fileAttributes.fileKey());
        assertFalse(fileAttributes.isRegularFile());
        assertFalse(fileAttributes.isDirectory());
        assertFalse(fileAttributes.isSymbolicLink());
        assertFalse(fileAttributes.isOther());
        assertEquals(0, fileAttributes.size());
        Assert.assertEquals(ObjectStorageFileAttributes.UNKNOWN_FILE_TIME, fileAttributes.lastModifiedTime());
    }

}
