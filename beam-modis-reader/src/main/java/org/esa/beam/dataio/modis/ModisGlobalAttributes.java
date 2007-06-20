package org.esa.beam.dataio.modis;

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.dataio.modis.hdf.HdfGlobalAttributes;
import org.esa.beam.framework.dataio.ProductIOException;

import java.awt.Dimension;
import java.util.Date;


public interface ModisGlobalAttributes {

    String getProductName();

    String getProductType();

    Dimension getProductDimensions();

    HdfDataField getDatafield(String name) throws ProductIOException;

    Date getSensingStart();

    Date getSensingStop();

    int[] getTiePointSubsAndOffset(String dimensionName) throws HDFException;

    void decode(final HdfGlobalAttributes hdfAttributes) throws ProductIOException;

    boolean isImappFormat();
}
