package org.esa.nest.dataio.sentinel1;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.dataio.imageio.ImageIOFile;

import java.io.IOException;

/**
 * Supports reading directories for level1, level2, and level0
 */
public interface Sentinel1Directory {

    public void close() throws IOException;

    public void readProductDirectory() throws IOException;

    public Product createProduct() throws IOException;

    public ImageIOFile.BandInfo getBandInfo(final Band destBand);

    public boolean isSLC();

    public boolean isOCN();
}
