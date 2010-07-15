package org.esa.beam.dataio.landsat.geotiff;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * Plugin class for the {@link LandsatGeotiffReader} reader.
 */
public class LandsatGeotiffReaderPlugin implements ProductReaderPlugIn {

    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class,File.class};

    private static final String[] FORMAT_NAMES = new String[]{"LandsatGeoTIFF"};
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".txt", ".TXT"};
    private static final String READER_DESCRIPTION = "Landsat Data Products (GeoTIFF)";
    private static final BeamFileFilter FILE_FILTER = new LandsatGeoTiffFileFilter();

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        File dir = getFileInput(input);
        File[] files = dir.listFiles();
        for (File file : files) {
            if (isMetadataFile(file)) {
                try {
                    LandsatMetadata landsatMetadata = new LandsatMetadata(new FileReader(file));
                    if (landsatMetadata.isLandsatTM()) {
                        return DecodeQualification.INTENDED;
                    }
                } catch (IOException ignore) {
                }
            }
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return READER_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new LandsatGeotiffReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return READER_DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return FILE_FILTER;
    }


    static File getFileInput(Object input) {
        if (input instanceof String) {
            return getFileInput(new File((String) input));
        } else if (input instanceof File) {
            return getFileInput((File) input);
        }
        return null;
    }

    static File getFileInput(File file) {
        if (file.isDirectory()) {
            return file;
        } else {
            return file.getParentFile();
        }
    }

    static boolean isMetadataFile(File file) {
        String filename = file.getName().toLowerCase();
        return filename.endsWith("_mtl.txt");
    }

    private static class LandsatGeoTiffFileFilter extends BeamFileFilter {

        public LandsatGeoTiffFileFilter() {
            super();
            setFormatName(FORMAT_NAMES[0]);
            setDescription(READER_DESCRIPTION);
        }

        @Override
        public boolean accept(final File file) {
            if (file.isDirectory()) {
                return true;
            }
            return isMetadataFile(file);
        }

    }
}