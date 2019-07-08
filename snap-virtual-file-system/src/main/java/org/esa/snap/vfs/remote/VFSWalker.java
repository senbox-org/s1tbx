package org.esa.snap.vfs.remote;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Walker for VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public interface VFSWalker {

    /**
     * Gets the VFS file basic attributes.
     *
     * @return The S3 file basic attributes
     * @throws IOException If an I/O error occurs
     */
    BasicFileAttributes readBasicFileAttributes(VFSPath path) throws IOException;

    /**
     * Gets a list of VFS files and directories from to the given prefix.
     *
     * @param dir The VFS path to traverse
     * @return The list of VFS files and directories
     * @throws IOException If an I/O error occurs
     */
    List<BasicFileAttributes> walk(VFSPath dir) throws IOException;

}
