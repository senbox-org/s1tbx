package org.esa.beam.binning.reader;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class BinnedProductReaderPlugin implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "Binned_data_product";
    public static final String FORMAT_DESCRIPTION = "SeaDAS-Level-3-alike NetCDF files containing binned Level-3 data";
    public static final String FILE_EXTENSION = ".nc";

    public DecodeQualification getDecodeQualification(Object input) {
        if (input == null) {
            return DecodeQualification.UNABLE;
        }
        final String path = input.toString();
        final String name = new File(path).getName();
        if (!BinnedFileFilter.isBinnedName(name)) {
            return DecodeQualification.UNABLE;
        }
        try {
            NetcdfFile netcdfFile = null;
            try {
                netcdfFile = NetcdfFile.open(path);
            } finally {
                if (netcdfFile != null) {
                    netcdfFile.close();
                }
            }
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        return DecodeQualification.INTENDED;
    }

    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    public ProductReader createReaderInstance() {
        return new BinnedProductReader(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return new BinnedFileFilter();
    }

    /**
     * Returns a string array containing the single entry <code>&quot;Binned data product&quot;</code>.
     */
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same length as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public String[] getDefaultFileExtensions() {
        return new String[]{FILE_EXTENSION};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale locale) {
        return "Reader for SeaDAS-Level-3-alike NetCDF files containing binned Level-3 data";
    }

}
