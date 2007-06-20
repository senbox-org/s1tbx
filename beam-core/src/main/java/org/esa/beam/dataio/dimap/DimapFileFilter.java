package org.esa.beam.dataio.dimap;

import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 15.11.2006
 * Time: 09:33:04
 * To change this template use File | Settings | File Templates.
 */
public class DimapFileFilter extends BeamFileFilter {
    public DimapFileFilter() {
        super(DimapProductConstants.DIMAP_FORMAT_NAME, DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION, "BEAM-DIMAP Files");
    }

    @Override
    public boolean accept(File file) {
        if (file == null || !file.isDirectory() || !file.getName().endsWith(
                DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION)) {
            return super.accept(file);
        }
        // file is the <name>.data directory:
        return hasDimapHeader(file);
    }


    private static boolean hasDimapHeader(File dataDir) {
        final File parentDir = dataDir.getParentFile();
        final File[] dimFiles = FileUtils.listFilesWithExtension(parentDir,
                                                                 DimapProductConstants.DIMAP_HEADER_FILE_EXTENSION);
        String baseName = FileUtils.getFilenameWithoutExtension(dataDir);
        if (dimFiles != null) {
            for (final File dimFile : dimFiles) {
                final String filenameWithoutExtension = FileUtils.getFilenameWithoutExtension(dimFile);
                if (baseName.equals(filenameWithoutExtension)) {
                    return false;
                }
            }
        }
        return true;
    }
}
