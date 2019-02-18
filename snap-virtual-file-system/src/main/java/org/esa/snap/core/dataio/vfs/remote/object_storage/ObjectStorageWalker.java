package org.esa.snap.core.dataio.vfs.remote.object_storage;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Walker for Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public interface ObjectStorageWalker {

    BasicFileAttributes getObjectStorageFile(String address, String prefix) throws IOException;

    List<BasicFileAttributes> walk(String prefix, String delimiter) throws IOException;

}
