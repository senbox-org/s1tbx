package org.esa.beam.binning;

import org.esa.beam.framework.datamodel.Product;

import java.awt.image.Raster;
import java.util.Iterator;

/**
 * A "slice" of observations. A slice is a spatially contiguous area of observations.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class ObservationSlice implements Iterable<Observation> {

    private Raster[] sourceTiles;
    private Raster maskTile;
    private Product product;
    private float[] superSamplingSteps;
    private DataPeriod dataPeriod;

    @Deprecated
    public ObservationSlice(Raster[] sourceTiles, Raster maskTile, Product product, float[] superSamplingSteps) {
        this(sourceTiles, maskTile, product, superSamplingSteps, null);
    }

    public ObservationSlice(Raster[] sourceTiles, Raster maskTile, Product product, float[] superSamplingSteps, DataPeriod dataPeriod) {
        this.sourceTiles = sourceTiles;
        this.maskTile = maskTile;
        this.product = product;
        this.superSamplingSteps = superSamplingSteps;
        this.dataPeriod = dataPeriod;
    }

    @Override
    public Iterator<Observation> iterator() {
        return ObservationIterator.create(sourceTiles, product, maskTile, superSamplingSteps, dataPeriod);
    }

}
