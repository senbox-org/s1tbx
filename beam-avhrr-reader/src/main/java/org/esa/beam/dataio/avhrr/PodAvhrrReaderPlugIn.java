package org.esa.beam.dataio.avhrr;

import org.esa.beam.dataio.avhrr.noaa.PodAvhrrFile;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * @author Ralf Quast
 */
public final class PodAvhrrReaderPlugIn implements ProductReaderPlugIn {

    private static final String DESCRIPTION = "NOAA Polar Orbiter Data Product";
    private static final String[] FILE_EXTENSIONS = new String[]{".l1b"};
    public static final String FORMAT_NAME = "NOAA_POD";
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME};
    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};

    public PodAvhrrReaderPlugIn() {
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = getInputFile(input);
        if (PodAvhrrFile.canDecode(file)) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    public static File getInputFile(Object input) {
        File file = null;
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        }
        return file;
    }

    @Override
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new PodAvhrrReader(this);
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }
}
