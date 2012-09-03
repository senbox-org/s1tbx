package org.esa.beam.binning;

import org.esa.beam.framework.datamodel.GeoCoding;

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
    private GeoCoding gc;
    private float[] superSamplingSteps;

    public ObservationSlice(Raster[] sourceTiles, Raster maskTile, GeoCoding gc, float[] superSamplingSteps) {
        this.sourceTiles = sourceTiles;
        this.maskTile = maskTile;
        this.gc = gc;
        this.superSamplingSteps = superSamplingSteps;
    }

    @Override
    public Iterator<Observation> iterator() {
        return ObservationIterator.create(sourceTiles, gc, maskTile, superSamplingSteps);
    }

}
