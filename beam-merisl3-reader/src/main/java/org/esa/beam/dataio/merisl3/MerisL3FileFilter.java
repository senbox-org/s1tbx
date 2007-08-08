package org.esa.beam.dataio.merisl3;

import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;

public class MerisL3FileFilter extends BeamFileFilter {

    public MerisL3FileFilter() {
        super(MerisL3ProductReaderPlugIn.FORMAT_NAME,
              MerisL3ProductReaderPlugIn.FILE_EXTENSION,
              MerisL3ProductReaderPlugIn.FORMAT_DESCRIPTION);
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }
        if (super.accept(file)) {
            return isMerisBinnedL3Name(file.getName());
        }
        return false;
    }

    /**
     * Checks if the given file name is valid.
     *
     * @param name the file name
     *
     * @return true, if so.
     */
    public static boolean isMerisBinnedL3Name(String name) {
        return name.startsWith("L3_ENV_MER_")
               && name.indexOf("_GLOB_SI_") != -1
               && name.endsWith(MerisL3ProductReaderPlugIn.FILE_EXTENSION);
    }

}
