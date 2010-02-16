package org.esa.beam.dataio.rtp;

import static org.esa.beam.dataio.rtp.RawTiledPyramidsProductCodecSpi.*;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;


class RawTiledPyramidsProductFileFilter extends BeamFileFilter {
    public RawTiledPyramidsProductFileFilter() {
        super(FORMAT_NAME, NO_FILE_EXTENSIONS, FORMAT_DESCRIPTION);
    }

    @Override
    public boolean accept(File file) {
        if (isProductDir(file.getParentFile())) {
            return file.getName().equals(HEADER_NAME);
        }
        return file.isDirectory();
    }

    @Override
    public boolean isCompoundDocument(File dir) {
        return isProductDir(dir);
    }

    @Override
    public FileSelectionMode getFileSelectionMode() {
        return FileSelectionMode.FILES_AND_DIRECTORIES;
    }
}
