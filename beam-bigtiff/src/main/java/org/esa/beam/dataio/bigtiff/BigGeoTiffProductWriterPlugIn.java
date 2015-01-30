package org.esa.beam.dataio.bigtiff;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

public class BigGeoTiffProductWriterPlugIn implements ProductWriterPlugIn {

    private static final Class[] OUTPUT_TYPES = new Class[]{String.class, File.class,};

    @Override
    public Class[] getOutputTypes() {
        return OUTPUT_TYPES;
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new BigGeoTiffProductWriter(this);
    }

    @Override
    public String[] getFormatNames() {
        return Constants.FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return Constants.FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return Constants.DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}
