package org.esa.snap.vfs.remote;

import org.esa.snap.vfs.NioPaths;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Test: Byte Channel for Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class AbstractRemoteFileSystemTest {

    private static final String FS_ID = "test:/TEST:/";

    @Test
    public void testFileSystemsNewFileSystem() throws Exception {
        Map<String, ?> env = new HashMap<>();
        URI uri = new URI(FS_ID);
        FileSystem fileSystem = FileSystems.newFileSystem(uri, env);
        assertTrue(fileSystem instanceof AbstractRemoteFileSystem);

        FileSystem fileSystem2 = FileSystems.getFileSystem(uri);
        assertSame(fileSystem, fileSystem2);

        fileSystem.close();

        try {
            FileSystems.getFileSystem(new URI(FS_ID));
            Assert.fail("Filesystem not closed.");
        } catch (FileSystemNotFoundException ignored) {
            //do nothing
        }
    }

    @Test
    public void testPathsGet() throws Exception {
        Path path = NioPaths.get(new URI(FS_ID + "hello/world/README.md"));
        Assert.assertNotNull(path);
        Assert.assertEquals("/TEST:/hello/world/README.md", path.toString());
    }

    @Test
    public void testDefaults() throws Exception {
        // To see how the default fs behaves
        System.out.println("###########################");
        Iterable<FileStore> fileStores = FileSystems.getDefault().getFileStores();
        for (FileStore fileStore : fileStores) {
            System.out.println("file stores:");
            System.out.println("  name = " + fileStore.name());
            System.out.println("  type = " + fileStore.type());
            System.out.println("  totalSpace = " + fileStore.getTotalSpace());
        }
        Path cwd = NioPaths.get("").toAbsolutePath();
        System.out.println("cwd: " + cwd);
        System.out.println("root: " + cwd.getRoot());
        Iterator<Path> iterator = Files.walk(cwd).iterator();
        while (iterator.hasNext()) {
            Path next = iterator.next();
            System.out.println("  " + next);
        }
        System.out.println("###########################");
    }

}
