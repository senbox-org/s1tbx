package org.esa.beam.dataio.dimap;

import static org.esa.beam.dataio.dimap.DimapProductConstants.*;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;


public class DimapFileFilter extends BeamFileFilter {
    public DimapFileFilter() {
        super(DIMAP_FORMAT_NAME, DIMAP_HEADER_FILE_EXTENSION, "BEAM-DIMAP product files");
    }

    @Override
    public boolean accept(File file) {
        if (file.isFile() && hasHeaderExt(file)) {
            return FileUtils.exchangeExtension(file, DIMAP_DATA_DIRECTORY_EXTENSION).isDirectory();
        } else {
            return file.isDirectory() && !isDataDir(file);
        }
    }

    @Override
    public boolean isCompoundDocument(File dir) {
        return isDataDir(dir);
    }

    private boolean isDataDir(File dir) {
        return hasDataExt(dir) && FileUtils.exchangeExtension(dir, DIMAP_HEADER_FILE_EXTENSION).isFile();
    }

    private boolean hasHeaderExt(File file) {
        return file.getName().endsWith(DIMAP_HEADER_FILE_EXTENSION);
    }

    private boolean hasDataExt(File file) {
        return file.getName().endsWith(DIMAP_DATA_DIRECTORY_EXTENSION);
    }
}
