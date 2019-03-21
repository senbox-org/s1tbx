package org.esa.snap.vfs.remote;

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

    /**
     * Gets the VFS file basic attributes.
     *
     * @param address The VFS service address
     * @param prefix  The VFS path to traverse
     * @return The S3 file basic attributes
     * @throws IOException If an I/O error occurs
     */
    BasicFileAttributes getObjectStorageFile(String address, String prefix) throws IOException;

    /**
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param prefix The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    List<BasicFileAttributes> walk(String prefix) throws IOException;

}
