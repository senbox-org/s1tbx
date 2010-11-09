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

    ///////////////////////////////////////////////
    // NetCdfWriteProfile related methods


    /**
     * Creates an instance of {@link org.esa.beam.dataio.netcdf.metadata.ProfileInitPartWriter}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.metadata.ProfileInitPartReader}
     */
    public abstract ProfileInitPartWriter createInitialisationPartWriter();

    /**
     * Creates an instance of {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.Product#getMetadataRoot() metadata}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} for metadata
     */
    public ProfilePartWriter createMetadataPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.Band bands}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} for bands
     */
    public ProfilePartWriter createBandPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.FlagCoding flag coding}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} for flag coding
     */
    public ProfilePartWriter createFlagCodingPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.GeoCoding geo-coding}.
     *
     * @return the {@link ProfilePartWriter} for geo-coding
     */
    public ProfilePartWriter createGeoCodingPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.ImageInfo image info}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} for image-info
     */
    public ProfilePartWriter createImageInfoPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.IndexCoding index coding}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} for index coding
     */
    public ProfilePartWriter createIndexCodingPartWriter() {
        return new NullProfilePartWriter();
    }
    /**
     * Creates an instance of {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.Mask masks}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} for masks
     */
    public ProfilePartWriter createMaskPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.Stx statistics}.
     *
     * @return the {@link ProfilePartWriter} for statistics
     */
    public ProfilePartWriter createStxPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.TiePointGrid tie-point grids}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} for tie-point grids
     */
    public ProfilePartWriter createTiePointGridPartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.Product#getStartTime()} and
     * {@link org.esa.beam.framework.datamodel.Product#getEndTime()}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} for time information
     */
    public ProfilePartWriter createTimePartWriter() {
        return new NullProfilePartWriter();
    }

    /**
     * Creates an instance of {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} responsible for writing
     * {@link org.esa.beam.framework.datamodel.Product#getDescription()}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter} for description
     */
    public ProfilePartWriter createDescriptionPartWriter() {
        return new NullProfilePartWriter();
    }
}
