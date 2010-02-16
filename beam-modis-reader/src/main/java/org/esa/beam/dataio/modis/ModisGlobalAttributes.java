package org.esa.beam.dataio.modis;

import org.esa.beam.dataio.modis.hdf.HdfDataField;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.GeoCoding;

import java.awt.Dimension;
import java.util.Date;


public interface ModisGlobalAttributes {

    String getProductName();

    String getProductType();

    Dimension getProductDimensions();

    HdfDataField getDatafield(String name) throws ProductIOException;

    Date getSensingStart();

    Date getSensingStop();

    int[] getSubsamplingAndOffset(String dimensionName);

    boolean isImappFormat();

    String getEosType();

    GeoCoding createGeocoding();
}
