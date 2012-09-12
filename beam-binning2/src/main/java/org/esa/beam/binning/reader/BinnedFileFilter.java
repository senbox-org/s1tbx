package org.esa.beam.binning.reader;

import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;

public class BinnedFileFilter extends BeamFileFilter {

    public BinnedFileFilter() {
        super(BinnedProductReaderPluginX.FORMAT_NAME,
              BinnedProductReaderPluginX.FILE_EXTENSION,
              BinnedProductReaderPluginX.FORMAT_DESCRIPTION);
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }
        if (super.accept(file)) {
            return isBinnedName(file.getName());
        }
        return false;
    }

    /**
     * Checks if the given file name is valid.
     *
     * @param name the file name
     * @return true, if so.
     */
    public static boolean isBinnedName(String name) {
        return name.indexOf("-bins") != -1
                && name.endsWith(BinnedProductReaderPluginX.FILE_EXTENSION);
    }

}
