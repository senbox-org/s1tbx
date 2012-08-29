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
    private int size;

    public ObservationSlice(Raster[] sourceTiles, Raster maskTile, GeoCoding gc, float[] superSamplingSteps) {
        this.sourceTiles = sourceTiles;
        this.maskTile = maskTile;
        this.gc = gc;
        this.superSamplingSteps = superSamplingSteps;
        this.size = maskTile.getWidth() * maskTile.getHeight() * superSamplingSteps.length * superSamplingSteps.length;
    }

    @Override
    public Iterator<Observation> iterator() {
//        if(superSamplingSteps.length == 1) {
//            return new ObservationIteratorWithoutSuperSampling(sourceTiles, maskTile, gc);
//        }
        return new ObservationIterator(sourceTiles, maskTile, superSamplingSteps, gc);
    }

    public int getSize() {
        return size;
    }

}
