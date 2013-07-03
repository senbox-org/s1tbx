package org.esa.beam.binning;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.Product;

import java.awt.Rectangle;
import java.util.Iterator;

/**
 * A "slice" of observations. A slice is a spatially contiguous area of observations.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class ObservationSlice implements Iterable<Observation> {

    private final MultiLevelImage[] sourceImages;
    private final MultiLevelImage maskImage;
    private final Product product;
    private final float[] superSamplingSteps;
    private final Rectangle sliceRect;
    private final DataPeriod dataPeriod;

    public ObservationSlice(MultiLevelImage[] sourceImages, MultiLevelImage maskImage, Product product,
                            float[] superSamplingSteps, Rectangle sliceRect, DataPeriod dataPeriod) {
        this.sourceImages = sourceImages;
        this.maskImage = maskImage;
        this.product = product;
        this.superSamplingSteps = superSamplingSteps;
        this.sliceRect = sliceRect;
        this.dataPeriod = dataPeriod;
    }

    @Override
    public Iterator<Observation> iterator() {
        return ObservationIterator.create(sourceImages, maskImage, product, superSamplingSteps, sliceRect, dataPeriod);
    }

}
