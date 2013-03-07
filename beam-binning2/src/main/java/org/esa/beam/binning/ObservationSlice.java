package org.esa.beam.binning;

import org.esa.beam.framework.datamodel.Product;

import java.awt.image.Raster;
import java.util.Iterator;

/**
 * A "slice" of observations. A slice is a spatially contiguous area of observations.
 *
 * @author Marco Peters
 */
public class ObservationSlice implements Iterable<Observation> {

    private Raster[] sourceTiles;
    private Raster maskTile;
    private Product product;
    private float[] superSamplingSteps;

    public ObservationSlice(Raster[] sourceTiles, Raster maskTile, Product product, float[] superSamplingSteps) {
        this.sourceTiles = sourceTiles;
        this.maskTile = maskTile;
        this.product = product;
        this.superSamplingSteps = superSamplingSteps;
    }

    @Override
    public Iterator<Observation> iterator() {
        return ObservationIterator.create(sourceTiles, product, maskTile, superSamplingSteps);
    }

}
