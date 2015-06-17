package org.esa.s1tbx.io.sentinel1;

import org.esa.s1tbx.io.imageio.ImageIOFile;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;

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
}
