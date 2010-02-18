package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;

/**
 * Represents a geo-coded scene. This interface is not ment to be implemented by clients.
 */
public interface Scene {

    void setGeoCoding(GeoCoding geoCoding);

    GeoCoding getGeoCoding();

    boolean transferGeoCodingTo(final Scene destScene, final ProductSubsetDef subsetDef);

    int getRasterWidth();

    int getRasterHeight();

    Product getProduct();
}

