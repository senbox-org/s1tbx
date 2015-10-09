package org.esa.s1tbx.io.sentinel1;

import org.esa.s1tbx.io.imageio.ImageIOFile;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

import java.io.IOException;

/**
 * Supports reading directories for level1, level2, and level0
 */
public interface Sentinel1Directory {

    void close() throws IOException;

    void readProductDirectory() throws IOException;

    Product createProduct() throws IOException;

    ImageIOFile.BandInfo getBandInfo(final Band destBand);

    boolean isSLC();
}
