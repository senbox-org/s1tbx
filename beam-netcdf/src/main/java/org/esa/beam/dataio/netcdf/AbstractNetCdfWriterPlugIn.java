package org.esa.beam.dataio.netcdf;

import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;

public abstract class AbstractNetCdfWriterPlugIn implements ProductWriterPlugIn {

    @Override
    public Class[] getOutputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new DefaultNetCdfWriter(this);
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    public abstract ProfileInitPartWriter createInitialisationPartWriter();

    public ProfilePartWriter createMetadataPartWriter() {
        return new NullProfilePartWriter();
    }

    public ProfilePartWriter createBandPartWriter() {
        return new NullProfilePartWriter();
    }

    public ProfilePartWriter createFlagCodingPartWriter() {
        return new NullProfilePartWriter();
    }

    public ProfilePartWriter createGeoCodingPartWriter() {
        return new NullProfilePartWriter();
    }

    public ProfilePartWriter createImageInfoPartWriter() {
        return new NullProfilePartWriter();
    }

    public ProfilePartWriter createIndexCodingPartWriter() {
        return new NullProfilePartWriter();
    }

    public ProfilePartWriter createMaskPartWriter() {
        return new NullProfilePartWriter();
    }

    public ProfilePartWriter createStxPartWriter() {
        return new NullProfilePartWriter();
    }

    public ProfilePartWriter createTiePointGridPartWriter() {
        return new NullProfilePartWriter();
    }

    public ProfilePartWriter createTimePartWriter() {
        return new NullProfilePartWriter();
    }

    public ProfilePartWriter createDescriptionPartWriter() {
        return new NullProfilePartWriter();
    }
}
